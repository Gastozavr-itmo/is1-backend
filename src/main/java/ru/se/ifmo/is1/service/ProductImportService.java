package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.se.ifmo.is1.dto.imports.ImportResponse;
import ru.se.ifmo.is1.dto.imports.OrganizationImportDTO;
import ru.se.ifmo.is1.dto.imports.PersonImportDTO;
import ru.se.ifmo.is1.dto.imports.ProductImportDTO;
import ru.se.ifmo.is1.dto.imports.ValidationError;
import ru.se.ifmo.is1.dto.shared.AddressDTO;
import ru.se.ifmo.is1.dto.shared.LocationDTO;
import ru.se.ifmo.is1.mapper.ImportMapper;
import ru.se.ifmo.is1.model.*;
import ru.se.ifmo.is1.ws.ChangePublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final SessionFactory sf;
    private final ImportHistoryService history;
    private final ChangePublisher changes;
    private final ImportMapper mapper;

    /** Чтобы два импорта не пересекались в один момент. */
    private static final ReentrantLock GLOBAL_IMPORT_LOCK = new ReentrantLock(true);

    private Session s() { return sf.getCurrentSession(); }


    @Transactional(rollbackFor = Exception.class)
    public ImportResponse importAllTransactional(List<ProductImportDTO> items) {
        final LocalDateTime startedAt = LocalDateTime.now();
        final List<PendingEvent> pendingEvents = new ArrayList<>();
        final Set<String> seen = new HashSet<>(); // защита от дублей в самом файле

        GLOBAL_IMPORT_LOCK.lock();
        try {
            int created = 0;
            List<ValidationError> errors = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                ProductImportDTO dto = items.get(i);
                try {
                    OrganizationImportDTO orgDto = dto.getManufacturer();
                    if (orgDto == null) throw new IllegalArgumentException("manufacturer is required");

                    String fullNameRaw = orgDto.getFullName();
                    if (fullNameRaw == null || fullNameRaw.isBlank())
                        throw new IllegalArgumentException("manufacturer.fullName required");

                    String normFullName = normalize(fullNameRaw);
                    Organization manufacturer = findOrganizationByFullName(normFullName);
                    if (manufacturer == null) {
                        Address official = mapAddress(orgDto.getOfficialAddress());
                        Address postal   = mapAddress(orgDto.getPostalAddress());
                        manufacturer = mapper.toOrganization(orgDto, official, postal);
                        manufacturer.setFullName(fullNameRaw.trim());
                        s().persist(manufacturer);
                        s().flush();
                        pendingEvents.add(PendingEvent.created("organization", manufacturer.getId()));
                    }

                    String partRaw = dto.getPartNumber();
                    String normPart = normalize(partRaw);
                    if (normPart == null || normPart.isBlank())
                        throw new IllegalArgumentException("partNumber required");

                    String mfgKey = normalize(manufacturer.getFullName());
                    String batchKey = mfgKey + "|" + normPart;

                    if (!seen.add(batchKey)) {
                        continue;
                    }
                    if (productExistsByFullNameAndSerial(mfgKey, normPart)) {
                        continue;
                    }

                    Person owner = null;
                    PersonImportDTO ownerDto = dto.getOwner();
                    if (ownerDto != null) {
                        Location loc = mapLocation(ownerDto.getLocation());
                        owner = mapper.toNullablePerson(ownerDto, loc);
                        if (owner != null) {
                            s().persist(owner);
                            s().flush();
                            pendingEvents.add(PendingEvent.created("person", owner.getId()));
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
                    p.setPartNumber(partRaw == null ? null : partRaw.trim());
                    p.setOwner(owner);

                    validateProduct(p);

                    try {
                        s().persist(p);
                        s().flush();
                    } catch (ConstraintViolationException e) {
                        continue;
                    }

                    pendingEvents.add(PendingEvent.created("product", p.getId()));
                    created++;

                } catch (Exception ex) {
                    errors.add(new ValidationError(
                            i,
                            "items[" + i + "]",
                            ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                    ));
                }
            }

            if (!errors.isEmpty()) {
                history.recordFailure(startedAt);
                throw new ImportFailedException(errors);
            }

            registerAfterCommitPublisher(pendingEvents);
            history.recordSuccess(startedAt, created);
            return ImportResponse.ok(created);

        } catch (ImportFailedException ife) {
            return ImportResponse.failed(0, ife.errors);
        } finally {
            GLOBAL_IMPORT_LOCK.unlock();
        }
    }


    private void registerAfterCommitPublisher(List<PendingEvent> events) {
        if (events.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (PendingEvent e : events) {
                    changes.broadcast(e.entity, e.action, e.id);
                }
            }
        });
    }

    private Organization findOrganizationByFullName(String normalizedFullName) {
        if (normalizedFullName == null) return null;
        return s().createQuery(
                        "select o from Organization o where lower(trim(o.fullName)) = :fn",
                        Organization.class)
                .setParameter("fn", normalizedFullName)
                .setMaxResults(1)
                .uniqueResult();
    }

    private boolean productExistsByFullNameAndSerial(String normFullName, String normPart) {
        Integer one = s().createQuery(
                        "select 1 " +
                                "from Product p join p.manufacturer m " +
                                "where lower(trim(m.fullName)) = :fn " +
                                "  and lower(trim(p.partNumber)) = :pn",
                        Integer.class)
                .setParameter("fn", normFullName)
                .setParameter("pn", normPart)
                .setMaxResults(1)
                .uniqueResult();
        return one != null;
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
        a.setTown(mapLocation(dto.getTown())); // Address.town = Location (в модели)
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
