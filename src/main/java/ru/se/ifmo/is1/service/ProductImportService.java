package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.se.ifmo.is1.concurrency.KeyedLockManager;
import ru.se.ifmo.is1.concurrency.LockKeys;
import ru.se.ifmo.is1.dto.imports.ImportResponse;
import ru.se.ifmo.is1.dto.imports.OrganizationImportDTO;
import ru.se.ifmo.is1.dto.imports.PersonImportDTO;
import ru.se.ifmo.is1.dto.imports.ProductImportDTO;
import ru.se.ifmo.is1.dto.imports.ValidationError;
import ru.se.ifmo.is1.dto.shared.AddressDTO;
import ru.se.ifmo.is1.dto.shared.LocationDTO;
import ru.se.ifmo.is1.mapper.ImportMapper;
import ru.se.ifmo.is1.model.*;
import ru.se.ifmo.is1.repository.OrganizationRepository;
import ru.se.ifmo.is1.repository.PersonRepository;
import ru.se.ifmo.is1.repository.ProductRepository;
import ru.se.ifmo.is1.ws.ChangePublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final ImportHistoryService history;
    private final ChangePublisher changes;
    private final ImportMapper mapper;

    private final OrganizationRepository organizationRepository;
    private final PersonRepository personRepository;
    private final ProductRepository productRepository;

    private final KeyedLockManager lockManager;

    private final SessionFactory sf;

    private Session s() {
        return sf.getCurrentSession();
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResponse importAllTransactional(List<ProductImportDTO> items) {
        final LocalDateTime startedAt = LocalDateTime.now();
        final List<PendingEvent> pendingEvents = new ArrayList<>();
        final Set<String> seen = new HashSet<>(); // защита от дублей в самом файле

        int created = 0;
        List<ValidationError> errors = new ArrayList<>();

        rowLoop:
        for (int i = 0; i < items.size(); i++) {
            ProductImportDTO dto = items.get(i);
            try {
                OrganizationImportDTO orgDto = dto.getManufacturer();
                if (orgDto == null) throw new IllegalArgumentException("manufacturer is required");

                String orgNameRaw = orgDto.getName();
                if (orgNameRaw == null || orgNameRaw.isBlank()) {
                    throw new IllegalArgumentException("manufacturer.name required");
                }

                String fullNameRaw = orgDto.getFullName();
                if (fullNameRaw == null || fullNameRaw.isBlank()) {
                    throw new IllegalArgumentException("manufacturer.fullName required");
                }

                String orgFullNameNorm = normalize(fullNameRaw);
                String orgLockKey = orgBusinessKey(orgNameRaw);

                Organization manufacturer;
                ReentrantLock orgLock = lockManager.get(orgLockKey);
                orgLock.lock();
                try {
                    manufacturer = organizationRepository.findByFullNameNormalized(orgFullNameNorm);
                    if (manufacturer == null) {
                        Address official = mapAddress(orgDto.getOfficialAddress());
                        Address postal = mapAddress(orgDto.getPostalAddress());
                        manufacturer = mapper.toOrganization(orgDto, official, postal);

                        if (manufacturer.getName() == null || manufacturer.getName().isBlank()) {
                            manufacturer.setName(orgNameRaw.trim());
                        }
                        manufacturer.setFullName(fullNameRaw.trim());

                        try {
                            Integer mid = organizationRepository.save(manufacturer);
                            pendingEvents.add(PendingEvent.created("organization", mid));
                        } catch (RuntimeException ex) {
                            if (isUniqueConstraint(ex)) {
                                s().clear();
                                manufacturer = organizationRepository.findByFullNameNormalized(orgFullNameNorm);
                                if (manufacturer == null) {
                                    errors.add(new ValidationError(
                                            i,
                                            "items[" + i + "].manufacturer",
                                            "Organization with fullName '" + fullNameRaw + "' already exists"
                                    ));
                                    continue rowLoop;
                                }
                            } else {
                                throw ex;
                            }
                        }
                    }
                } finally {
                    orgLock.unlock();
                }

                String partRaw = dto.getPartNumber();
                if (partRaw == null || partRaw.isBlank())
                    throw new IllegalArgumentException("partNumber required");

                String normMfgName = normalize(orgNameRaw);
                String normPart = normalize(partRaw);

                String fileKey = normMfgName + "|" + normPart;
                if (!seen.add(fileKey)) {
                    continue;
                }
                Person owner = null;
                PersonImportDTO ownerDto = dto.getOwner();
                if (ownerDto != null) {
                    Location loc = mapLocation(ownerDto.getLocation());
                    Person candidate = mapper.toNullablePerson(ownerDto, loc);
                    if (candidate != null) {
                        String personLockKey = personBusinessKey(candidate);

                        ReentrantLock personLock = lockManager.get(personLockKey);
                        personLock.lock();
                        try {
                            Person existingOwner = personRepository.findByBusinessKey(
                                    normalize(candidate.getName()),
                                    candidate.getNationality(),
                                    candidate.getLocation() != null ? candidate.getLocation().getX() : null,
                                    candidate.getLocation() != null ? candidate.getLocation().getY() : null,
                                    candidate.getLocation() != null ? normalize(candidate.getLocation().getName()) : null
                            );
                            if (existingOwner != null) {
                                owner = existingOwner;
                            } else {
                                Long ownerId = personRepository.save(candidate);
                                owner = candidate;
                                pendingEvents.add(PendingEvent.created("person", ownerId));
                            }
                        } finally {
                            personLock.unlock();
                        }
                    }
                }

                Product p = new Product();
                p.setName(dto.getName());
                p.setCoordinates(mapper.toCoordinates(dto.getCoordinates()));
                p.setCreationDate(new Date());
                p.setUnitOfMeasure(parseUnit(dto.getUnitOfMeasure()));
                p.setManufacturer(manufacturer);

                if (dto.getPrice() == null) throw new IllegalArgumentException("price required");
                p.setPrice(dto.getPrice());
                p.setManufactureCost(dto.getManufactureCost() == null ? 0 : dto.getManufactureCost());
                p.setRating(dto.getRating() == null ? 0 : dto.getRating());
                p.setPartNumber(partRaw.trim());
                p.setOwner(owner);

                validateProduct(p);

                String productLockKey = productBusinessKey(partRaw, manufacturer);
                ReentrantLock productLock = lockManager.get(productLockKey);
                productLock.lock();
                try {
                    Product existing = productRepository.findByManufacturerAndNormalizedPartNumber(
                            manufacturer, normPart
                    );
                    if (existing != null) {
                        continue;
                    }

                    try {
                        Long pid = productRepository.save(p);
                        pendingEvents.add(PendingEvent.created("product", pid));
                        created++;
                    } catch (RuntimeException ex) {
                        if (isUniqueConstraint(ex)) {
                            s().clear();
                            errors.add(new ValidationError(
                                    i,
                                    "items[" + i + "]",
                                    "Product with same partNumber and manufacturer already exists"
                            ));
                            continue rowLoop;
                        } else {
                            throw ex;
                        }
                    }
                } finally {
                    productLock.unlock();
                }

            } catch (Exception ex) {
                if (isUniqueConstraint(ex)) {
                    s().clear();
                }
                errors.add(new ValidationError(
                        i,
                        "items[" + i + "]",
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                ));
            }
        }

        if (!errors.isEmpty()) {
            history.recordFailure(startedAt);
            return ImportResponse.failed(0, errors);
        }

        registerAfterCommitPublisher(pendingEvents);
        history.recordSuccess(startedAt, created);
        return ImportResponse.ok(created);
    }

    private void registerAfterCommitPublisher(List<PendingEvent> events) {
        if (events.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (PendingEvent e : events) {
                    changes.broadcast(e.entity, e.action, e.id);
                }
            }
        });
    }


    private String orgBusinessKey(String rawName) {
        return "org:uniq:name:" + LockKeys.norm(rawName);
    }

    private String personBusinessKey(Person p) {
        return "person:uniq:" + LockKeys.personKey(
                p.getName(),
                p.getNationality(),
                p.getLocation() != null ? p.getLocation().getName() : null
        );
    }

    private String productBusinessKey(String partNumber, Organization m) {
        Integer mid = (m != null ? m.getId() : null);
        return "product:uniq:" + LockKeys.productKey(partNumber, mid);
    }

    private UnitOfMeasure parseUnit(String unit) {
        if (unit == null) throw new IllegalArgumentException("unitOfMeasure required");
        try {
            return UnitOfMeasure.valueOf(unit.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown unitOfMeasure: " + unit);
        }
    }

    private Address mapAddress(AddressDTO dto) {
        if (dto == null) return null;
        Address a = new Address();
        a.setZipCode(dto.getZipCode());
        a.setTown(mapLocation(dto.getTown()));
        return a;
    }

    private Location mapLocation(LocationDTO dto) {
        if (dto == null) return null;
        Location l = new Location();
        l.setX(dto.getX());
        l.setY(dto.getY());
        l.setName(dto.getName());
        return l;
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private void validateProduct(Product p) {
        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("name required");
        if (p.getCoordinates() == null
                || p.getCoordinates().getX() == null
                || p.getCoordinates().getY() == null)
            throw new IllegalArgumentException("coordinates x,y required");
        if (p.getCoordinates().getX() > 450)
            throw new IllegalArgumentException("coordinates.x must be <= 450");
        if (p.getCoordinates().getY() <= -422)
            throw new IllegalArgumentException("coordinates.y must be > -422");
        if (p.getUnitOfMeasure() == null)
            throw new IllegalArgumentException("unitOfMeasure required");
        if (p.getManufacturer() == null)
            throw new IllegalArgumentException("manufacturer required");
        if (p.getPrice() <= 0)
            throw new IllegalArgumentException("price > 0 required");
        if (p.getRating() <= 0)
            throw new IllegalArgumentException("rating > 0 required");
        if (p.getPartNumber() == null || p.getPartNumber().trim().isEmpty())
            throw new IllegalArgumentException("partNumber required");
    }

    private boolean isUniqueConstraint(Throwable t) {
        while (t != null) {
            if (t instanceof ConstraintViolationException cve) {
                String state = cve.getSQLState();
                if ("23505".equals(state) || "23503".equals(state)) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private record PendingEvent(String entity, String action, Number id) {
        static PendingEvent created(String entity, Number id) {
            return new PendingEvent(entity, "created", id);
        }
    }

    private static class ImportFailedException extends RuntimeException {
        final List<ValidationError> errors;

        ImportFailedException(List<ValidationError> errors) {
            super("Import failed");
            this.errors = errors;
        }
    }
}
