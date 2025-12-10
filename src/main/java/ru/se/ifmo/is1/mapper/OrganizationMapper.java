package ru.se.ifmo.is1.mapper;

import org.springframework.stereotype.Component;
import ru.se.ifmo.is1.dto.organization.OrganizationCreateDTO;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.shared.AddressDTO;
import ru.se.ifmo.is1.dto.shared.LocationDTO;
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

    private Address toAddress(AddressDTO dto) {
        if (dto == null) return null;
        Address a = new Address();
        a.setZipCode(dto.getZipCode());
        LocationDTO t = dto.getTown();
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
        if (o == null) return null;

        return OrganizationViewDTO.builder()
                .id(o.getId())
                .name(o.getName())
                .officialAddress(toAddressDTO(o.getOfficialAddress()))
                .postalAddress(toAddressDTO(o.getPostalAddress()))
                .annualTurnover(o.getAnnualTurnover())
                .employeesCount(o.getEmployeesCount())
                .fullName(o.getFullName())
                .rating(o.getRating())
                .build();
    }

    private AddressDTO toAddressDTO(Address a) {
        if (a == null) return null;
        return AddressDTO.builder()
                .zipCode(a.getZipCode())
                .town(toLocationDTO(a.getTown()))
                .build();
    }

    private LocationDTO toLocationDTO(Location t) {
        if (t == null) return null;
        return LocationDTO.builder()
                .x(t.getX())
                .y(t.getY())
                .name(t.getName())
                .build();
    }
}
