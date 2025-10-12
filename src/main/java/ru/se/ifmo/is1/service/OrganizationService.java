package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.dto.organization.OrganizationCreateDTO;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.paging.PageRequestDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.mapper.OrganizationMapper;
import ru.se.ifmo.is1.model.Address;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Organization;
import ru.se.ifmo.is1.repository.OrganizationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository repo;
    private final OrganizationMapper mapper;

    @Transactional(readOnly = true)
    public OrganizationViewDTO get(Integer id) {
        return mapper.toView(repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Organization not found")));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<OrganizationViewDTO> list(PageRequestDTO pr){
        var items = repo.findPageNative(pr.offset(), pr.getSize(), pr.getSort(), pr.getDir())
                .stream().map(mapper::toView).toList();
        long total = repo.countAllNative();
        return PageResponseDTO.of(items, pr.getPage(), pr.getSize(), total, pr.getSort(), pr.getDir());
    }




    @Transactional
    public Integer create(OrganizationCreateDTO dto) {
        Organization o = mapper.toEntity(dto);
        validate(o);
        return repo.save(o);
    }

    @Transactional
    public void update(Integer id, OrganizationCreateDTO dto) {
        Organization o = mapper.toEntity(dto);
        o.setId(id);
        validate(o);
        repo.merge(o);
    }

    @Transactional
    public void delete(Integer id) {
        var e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        repo.delete(e);
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
