package ru.se.ifmo.is1.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.se.ifmo.is1.dto.imports.ImportResponse;
import ru.se.ifmo.is1.dto.imports.ProductImportDTO;
import ru.se.ifmo.is1.mapper.ImportMapper;
import ru.se.ifmo.is1.model.*;
import ru.se.ifmo.is1.repository.OrganizationRepository;
import ru.se.ifmo.is1.repository.PersonRepository;
import ru.se.ifmo.is1.repository.ProductRepository;
import ru.se.ifmo.is1.util.NormalizationUtil;
import ru.se.ifmo.is1.ws.ChangePublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final SessionFactory sessionFactory;
    private final ImportMapper mapper;
    private final ImportHistoryService history;

    private final OrganizationRepository orgRepo;
    private final PersonRepository personRepo;
    private final ProductRepository productRepo;

    private final ChangePublisher changes;

    private org.hibernate.Session s() {
        return sessionFactory.getCurrentSession();
    }

    private record PendingEvent(String entity, String action, Supplier<Long> id) {
    }

    @Retryable(
            retryFor = {
                    CannotAcquireLockException.class,
                    TransactionSystemException.class,
                    OptimisticLockException.class
            },
            noRetryFor = {
                    IllegalArgumentException.class,
                    org.hibernate.exception.ConstraintViolationException.class,
                    org.springframework.dao.DataIntegrityViolationException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(delay = 20)
    )
    @Transactional(rollbackFor = Exception.class, isolation = SERIALIZABLE)
    public ImportResponse importAllTransactional(List<ProductImportDTO> items) {
        final LocalDateTime startedAt = LocalDateTime.now();
        int created = 0;

        Map<String, Organization> orgCache = new HashMap<>();
        List<PendingEvent> events = new ArrayList<>();

        for (ProductImportDTO dto : items) {

            if (dto.getManufacturer() == null) {
                throw new IllegalArgumentException("manufacturer is required");
            }
            var orgDto = dto.getManufacturer();
            if (orgDto.getFullName() == null || orgDto.getFullName().isBlank()) {
                throw new IllegalArgumentException("manufacturer.fullName must not be null/blank");
            }

            String orgKeyNorm = NormalizationUtil.canonicalKey(orgDto.getFullName(), true);

            Organization org = orgCache.get(orgKeyNorm);
            if (org == null) {
                org = orgRepo.findByFullNameNormalized(orgKeyNorm);

                if (org == null) {
                    var official = mapper.toAddress(orgDto.getOfficialAddress());
                    var postal = mapper.toAddress(orgDto.getPostalAddress());
                    org = mapper.toOrganization(orgDto, official, postal);

                    validateOrganization(org);

                    s().persist(org);

                    Organization orgRef = org;
                    events.add(new PendingEvent(
                            "organization",
                            "created",
                            () -> (long) orgRef.getId()
                    ));
                }

                orgCache.put(orgKeyNorm, org);
            }

            Person owner = null;
            if (dto.getOwner() != null) {
                var ownerLoc = mapper.toNullableLocation(dto.getOwner().getLocation());
                var ownerTmp = mapper.toNullablePerson(dto.getOwner(), ownerLoc);

                validatePerson(ownerTmp);

                String nameNorm = NormalizationUtil.canonicalKey(ownerTmp.getName(), true);
                Person existingOwner = personRepo.findByBusinessKey(nameNorm);

                if (existingOwner != null) {
                    owner = existingOwner;
                } else {
                    s().persist(ownerTmp);
                    owner = ownerTmp;

                    Person ownerRef = owner;
                    events.add(new PendingEvent(
                            "person",
                            "created",
                            () -> (long) ownerRef.getId()
                    ));
                }
            }

            var coords = mapper.toCoordinates(dto.getCoordinates());
            var product = mapper.toProduct(dto, org, owner, coords);

            validateProduct(product);

            String partNumberNorm = NormalizationUtil.canonicalPartNumber(product.getPartNumber());
            Product existingProduct = productRepo.findByBusinessKey(org, partNumberNorm);

            if (existingProduct != null) {
                continue;
            }

            s().persist(product);
            created++;

            Product productRef = product;
            events.add(new PendingEvent(
                    "product",
                    "created",
                    () -> (long) productRef.getId()
            ));
        }
        s().flush();

        final int createdFinal = created;
        final List<PendingEvent> safeEvents = List.copyOf(events);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (PendingEvent e : safeEvents) {
                    changes.broadcast(e.entity(), e.action(), e.id().get());
                }
                history.recordSuccess(startedAt, createdFinal);
                changes.broadcast("imports", "updated", null);
            }
        });

        return ImportResponse.ok(created);
    }

    @Recover
    public ImportResponse recover(Exception ex, List<ProductImportDTO> items) {
        history.recordFailure(LocalDateTime.now());
        changes.broadcast("imports", "updated", null);

        throw (ex instanceof RuntimeException re) ? re : new RuntimeException(ex);
    }


    private void validateOrganization(Organization o) {
        if (o.getName() == null || o.getName().trim().isEmpty())
            throw new IllegalArgumentException("manufacturer.name required");
        if (o.getAnnualTurnover() == null || o.getAnnualTurnover() <= 0)
            throw new IllegalArgumentException("manufacturer.annualTurnover > 0 required");
        if (o.getEmployeesCount() <= 0)
            throw new IllegalArgumentException("manufacturer.employeesCount > 0 required");
        if (o.getRating() <= 0)
            throw new IllegalArgumentException("manufacturer.rating > 0 required");

        requireAddress(o.getOfficialAddress(), "manufacturer.officialAddress");
        requireAddress(o.getPostalAddress(), "manufacturer.postalAddress");
    }

    private void requireAddress(Address a, String n) {
        if (a == null) throw new IllegalArgumentException(n + " required");
        if (a.getZipCode() == null || a.getZipCode().trim().isEmpty())
            throw new IllegalArgumentException(n + ".zipCode required");
        Location t = a.getTown();
        if (t == null || t.getX() == null || t.getY() == null ||
                t.getName() == null || t.getName().trim().isEmpty())
            throw new IllegalArgumentException(n + ".town x,y,name required");
    }

    private void validatePerson(Person p) {
        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("owner.name required");
        if (p.getHeight() <= 0)
            throw new IllegalArgumentException("owner.height > 0 required");
        if (p.getNationality() == null)
            throw new IllegalArgumentException("owner.nationality required");
        Location l = p.getLocation();
        if (l != null) {
            if (l.getX() == null || l.getY() == null)
                throw new IllegalArgumentException("owner.location x,y required if present");
            if (l.getName() == null || l.getName().trim().isEmpty())
                throw new IllegalArgumentException("owner.location.name required");
        }
    }

    private void validateProduct(Product p) {
        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("product.name required");

        Coordinates c = p.getCoordinates();
        if (c == null) throw new IllegalArgumentException("product.coordinates required");
        if (c.getX() == null || c.getX() > 450)
            throw new IllegalArgumentException("product.coordinates.x must be <= 450");
        if (c.getY() == null || c.getY() <= -422)
            throw new IllegalArgumentException("product.coordinates.y must be > -422");

        if (p.getUnitOfMeasure() == null)
            throw new IllegalArgumentException("product.unitOfMeasure required");
        if (p.getManufacturer() == null)
            throw new IllegalArgumentException("product.manufacturer required");
        if (p.getPrice() <= 0)
            throw new IllegalArgumentException("product.price > 0 required");
        if (p.getRating() <= 0)
            throw new IllegalArgumentException("product.rating > 0 required");
        if (p.getPartNumber() == null || p.getPartNumber().trim().isEmpty())
            throw new IllegalArgumentException("product.partNumber required");
    }
}
