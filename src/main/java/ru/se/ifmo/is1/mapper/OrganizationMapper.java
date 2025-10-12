package ru.se.ifmo.is1.mapper;

import org.springframework.stereotype.Component;
import ru.se.ifmo.is1.dto.organization.OrganizationCreateDTO;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.shared.AddressCreateDTO;
import ru.se.ifmo.is1.dto.shared.TownCreateDTO;
import ru.se.ifmo.is1.model.Address;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Organization;

@Component
public class OrganizationMapper {

    public Organization toEntity(OrganizationCreateDTO dto) {
        Organization o = new Organization();
        o.setName(dto.getName());
        o.setAnnualTurnover(dto.getAnnualTurnover());
        o.setEmployeesCount(dto.getEmployeesCount());
        o.setFullName(dto.getFullName());
        o.setRating(dto.getRating());
        o.setOfficialAddress(toAddress(dto.getOfficialAddress()));
        o.setPostalAddress(toAddress(dto.getPostalAddress()));
        return o;
    }

    private Address toAddress(AddressCreateDTO dto) {
        if (dto == null) return null;
        Address a = new Address();
        a.setZipCode(dto.getZipCode());
        TownCreateDTO t = dto.getTown();
        if (t != null) {
            Location loc = new Location();
            loc.setX(t.getX());
            loc.setY(t.getY());
            loc.setName(t.getName());
            a.setTown(loc);
        }
        return a;
    }

    public OrganizationViewDTO toView(Organization o) {
        return OrganizationViewDTO.builder()
                .id(o.getId())
                .name(o.getName())
                .build();
    }
}
