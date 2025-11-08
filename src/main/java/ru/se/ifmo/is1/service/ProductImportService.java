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

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private final SessionFactory sessionFactory;
    private final ImportMapper mapper;
    private final ImportHistoryService history;

    private final OrganizationRepository orgRepo;
    private final PersonRepository personRepo;
    private final ProductRepository productRepo;

    private org.hibernate.Session s() { return sessionFactory.getCurrentSession(); }

    @Transactional(rollbackFor = Exception.class)
    public ImportResponse importAllTransactional(List<ProductImportDTO> items) {
        final LocalDateTime startedAt = LocalDateTime.now();
        int created = 0;

        Map<String, Organization> orgCache = new HashMap<>();

        try {
            for (int i = 0; i < items.size(); i++) {
                var dto = items.get(i);

                var orgDto = dto.getManufacturer();
                if (orgDto == null || orgDto.getFullName() == null || orgDto.getFullName().isBlank()) {
                    throw new IllegalArgumentException("manufacturer.fullName must not be null/blank");
                }
                String orgKeyNorm = NormalizationUtil.canonicalKey(orgDto.getFullName(), true);

                Organization org = orgCache.get(orgKeyNorm);
                if (org == null) {
                    org = orgRepo.findByFullNameNormalized(orgKeyNorm);
                    if (org == null) {
                        var official = mapper.toAddress(orgDto.getOfficialAddress());
                        var postal   = mapper.toAddress(orgDto.getPostalAddress());
                        org = mapper.toOrganization(orgDto, official, postal);
                        s().persist(org);
                    }
                    orgCache.put(orgKeyNorm, org);
                }

                Person owner = null;
                if (dto.getOwner() != null) {
                    var ownerLoc = mapper.toNullableLocation(dto.getOwner().getLocation());
                    var ownerTmp = mapper.toNullablePerson(dto.getOwner(), ownerLoc);

                    String nameNorm = NormalizationUtil.canonicalKey(ownerTmp.getName(), true);
                    String locNameNorm = ownerTmp.getLocation() != null && ownerTmp.getLocation().getName() != null
                            ? NormalizationUtil.canonicalKey(ownerTmp.getLocation().getName(), true)
                            : null;
                    Long lx = ownerTmp.getLocation() != null ? ownerTmp.getLocation().getX() : null;
                    Long ly = ownerTmp.getLocation() != null ? ownerTmp.getLocation().getY() : null;

                    var existing = personRepo.findByBusinessKey(
                            nameNorm, ownerTmp.getNationality(), lx, ly, locNameNorm);
                    if (existing != null) {
                        owner = existing;
                    } else {
                        s().persist(ownerTmp);
                        owner = ownerTmp;
                    }
                }

                var coords  = mapper.toCoordinates(dto.getCoordinates());
                var product = mapper.toProduct(dto, org, owner, coords);

                String partNumberNorm = NormalizationUtil.canonicalPartNumber(product.getPartNumber());
                var existingProduct = productRepo.findByManufacturerAndNormalizedPartNumber(org, partNumberNorm);
                if (existingProduct != null) {
                    continue;
                }

                s().persist(product);
                created++;
            }

            s().flush();

            final int createdFinal = created;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    history.recordSuccess(startedAt, createdFinal);
                }
            });

            return ImportResponse.ok(created);

        } catch (Exception ex) {
            history.recordFailure(startedAt);
            throw ex;
        }
    }
}
