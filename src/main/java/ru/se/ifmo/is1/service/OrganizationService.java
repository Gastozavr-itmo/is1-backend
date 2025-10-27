package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.dto.organization.OrganizationCreateDTO;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;

import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.mapper.OrganizationMapper;
import ru.se.ifmo.is1.model.Address;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Organization;
import ru.se.ifmo.is1.repository.OrganizationRepository;
import ru.se.ifmo.is1.ws.ChangePublisher;


@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository repo;
    private final OrganizationMapper mapper;
    private final ChangePublisher changes;

    @Transactional(readOnly = true)
    public OrganizationViewDTO get(Integer id) {
        return mapper.toView(repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Organization not found")));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<OrganizationViewDTO> list(
            int page,
            int size,
            String sort,
            String dir,
            String name,
            String fullName,
            String officialTownName,
            String postalTownName
    ) {
        int offset = Math.max(page, 0) * Math.max(size, 1);

        var rows  = repo.findFiltered(name, fullName, officialTownName, postalTownName, offset, size, sort, dir);
        long total = repo.countFiltered(name, fullName, officialTownName, postalTownName);

        var items = rows.stream().map(mapper::toView).toList();
        return PageResponseDTO.of(items, page, size, total, sort, dir);
    }




    @Transactional
    public Integer create(OrganizationCreateDTO dto) {
        Organization o = mapper.toEntity(dto);
        validate(o);
        Integer id = repo.save(o);
        changes.broadcast("organization", "created", id);
        return id;
    }

    @Transactional
    public void update(Integer id, OrganizationCreateDTO dto) {
        Organization o = mapper.toEntity(dto);
        o.setId(id);
        validate(o);
        repo.merge(o);
        changes.broadcast("organization", "updated", id);
    }

    @Transactional
    public void delete(Integer id) {
        var e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        repo.delete(e);
        changes.broadcast("organization", "deleted", id);
    }

    private void validate(Organization o) {
        if (o.getName() == null || o.getName().trim().isEmpty())
            throw new IllegalArgumentException("name required");
        if (o.getAnnualTurnover() == null || o.getAnnualTurnover() <= 0)
            throw new IllegalArgumentException("annualTurnover > 0 required");
        if (o.getEmployeesCount() <= 0)
            throw new IllegalArgumentException("employeesCount > 0 required");
        if (o.getRating() <= 0)
            throw new IllegalArgumentException("rating > 0 required");
        requireAddress(o.getOfficialAddress(), "officialAddress");
        requireAddress(o.getPostalAddress(), "postalAddress");
    }

    private void requireAddress(Address a, String n) {
        if (a == null) throw new IllegalArgumentException(n + " required");
        if (a.getZipCode() == null || a.getZipCode().trim().isEmpty())
            throw new IllegalArgumentException(n + ".zipCode required");
        Location t = a.getTown();
        if (t == null || t.getX() == null || t.getY() == null || t.getName() == null || t.getName().trim().isEmpty())
            throw new IllegalArgumentException(n + ".town x,y,name required");
    }
}
