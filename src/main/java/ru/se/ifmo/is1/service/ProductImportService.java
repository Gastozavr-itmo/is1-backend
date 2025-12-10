package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

    @Transactional(rollbackFor = Exception.class, isolation = SERIALIZABLE)
    public ImportResponse importAllTransactional(List<ProductImportDTO> items) {
        final LocalDateTime startedAt = LocalDateTime.now();

        final AtomicInteger created = new AtomicInteger(0);
        final List<PendingEvent> events = new ArrayList<>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                List<PendingEvent> safeEvents = List.copyOf(events);
                int createdFinal = created.get();

                for (PendingEvent e : safeEvents) {
                    changes.broadcast(e.entity(), e.action(), e.id().get());
                }

                history.recordSuccess(startedAt, createdFinal);
                changes.broadcast("imports", "" +
                        "", null);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    history.recordFailure(startedAt);
                    changes.broadcast("imports", "updated", null);
                }
            }
        });

        try {
            Map<String, Organization> orgCache = new HashMap<>();
            Map<String, Person> personCache = new HashMap<>();
            Set<String> productKeysInBatch = new HashSet<>();

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

                    String ownerKeyNorm = NormalizationUtil.canonicalKey(ownerTmp.getName(), true);

                    owner = personCache.get(ownerKeyNorm);
                    if (owner == null) {
                        Person existingOwner = personRepo.findByBusinessKey(ownerKeyNorm);
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
                        personCache.put(ownerKeyNorm, owner);
                    }
                }

                var coords = mapper.toCoordinates(dto.getCoordinates());
                var product = mapper.toProduct(dto, org, owner, coords);

                validateProduct(product);

                String partNumberNorm = NormalizationUtil.canonicalPartNumber(product.getPartNumber());

                String productKey = orgKeyNorm + "#" + partNumberNorm;

                if (!productKeysInBatch.add(productKey)) {
                    throw new IllegalStateException(
                            "Product with partNumber '" + product.getPartNumber() +
                                    "' for manufacturer '" + org.getFullName() +
                                    "' appears more than once in import batch");
                }

                Product existingProduct = productRepo.findByBusinessKey(org, partNumberNorm);
                if (existingProduct != null) {
                    throw new IllegalStateException(
                            "Product with partNumber '" + product.getPartNumber() +
                                    "' for manufacturer '" + org.getFullName() +
                                    "' already exists (id=" + existingProduct.getId() + ")");
                }

                s().persist(product);
                created.incrementAndGet();

                Product productRef = product;
                events.add(new PendingEvent(
                        "product",
                        "created",
                        () -> (long) productRef.getId()
                ));
            }

            s().flush();

            return ImportResponse.ok(created.get());
        } catch (Throwable ex) {
            errorRef.set(ex);
            throw ex;
        }
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
