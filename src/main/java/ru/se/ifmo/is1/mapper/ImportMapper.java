package ru.se.ifmo.is1.mapper;

import org.springframework.stereotype.Component;

// ==== Импортные/CRUD DTO ====
import ru.se.ifmo.is1.dto.imports.OrganizationImportDTO;
import ru.se.ifmo.is1.dto.imports.PersonImportDTO;
import ru.se.ifmo.is1.dto.imports.ProductImportDTO;

import ru.se.ifmo.is1.dto.organization.OrganizationCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;

import ru.se.ifmo.is1.dto.shared.AddressDTO;
import ru.se.ifmo.is1.dto.shared.CoordinatesDTO;
import ru.se.ifmo.is1.dto.shared.LocationDTO;

import ru.se.ifmo.is1.model.Address;
import ru.se.ifmo.is1.model.Coordinates;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Organization;
import ru.se.ifmo.is1.model.Person;
import ru.se.ifmo.is1.model.Product;

import ru.se.ifmo.is1.model.Color;
import ru.se.ifmo.is1.model.Country;
import ru.se.ifmo.is1.model.UnitOfMeasure;

@Component
public class ImportMapper {

    private Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.valueOf(v.toString());
    }

    private Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.valueOf(v.toString());
    }

    private Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.valueOf(v.toString());
    }

    public Coordinates toCoordinates(CoordinatesDTO dto) {
        if (dto == null) return null;
        Coordinates e = new Coordinates();

        Double x = asDouble(dto.getX());
        Long y = asLong(dto.getY());
        if (x == null || y == null) {
            throw new IllegalArgumentException("coordinates.x and coordinates.y must not be null");
        }
        e.setX(x);
        e.setY(y);
        return e;
    }

    public Location toLocation(LocationDTO dto) {
        if (dto == null) return null;
        Location e = new Location();

        if (dto.getName() == null || dto.getName().trim().isEmpty())
            throw new IllegalArgumentException("location.name must not be blank");

        Long x = asLong(dto.getX());
        Long y = asLong(dto.getY());
        if (x == null || y == null)
            throw new IllegalArgumentException("location.x and location.y must not be null");

        e.setName(dto.getName());
        e.setX(x);
        e.setY(y);
        return e;
    }

    public Location toNullableLocation(LocationDTO dto) {
        return dto == null ? null : toLocation(dto);
    }

    public Address toAddress(AddressDTO dto) {
        if (dto == null) return null;
        Address e = new Address();

        if (dto.getZipCode() == null || dto.getZipCode().trim().isEmpty())
            throw new IllegalArgumentException("address.zipCode must not be blank");
        if (dto.getTown() == null)
            throw new IllegalArgumentException("address.town must not be null");

        e.setZipCode(dto.getZipCode());
        e.setTown(toLocation(dto.getTown()));
        return e;
    }

    public Organization toOrganization(OrganizationImportDTO dto, Address official, Address postal) {
        if (dto == null) return null;
        Organization e = new Organization();

        if (dto.getName() == null || dto.getName().trim().isEmpty())
            throw new IllegalArgumentException("organization.name must not be blank");
        if (dto.getAnnualTurnover() == null || dto.getAnnualTurnover() <= 0)
            throw new IllegalArgumentException("organization.annualTurnover must be > 0");
        if (dto.getEmployeesCount() == null || dto.getEmployeesCount() <= 0)
            throw new IllegalArgumentException("organization.employeesCount must be > 0");
        if (dto.getRating() == null || dto.getRating() <= 0)
            throw new IllegalArgumentException("organization.rating must be > 0");

        e.setName(dto.getName());
        e.setFullName(dto.getFullName());
        e.setAnnualTurnover(dto.getAnnualTurnover());
        e.setEmployeesCount(asInt(dto.getEmployeesCount()));
        e.setRating(dto.getRating());
        e.setOfficialAddress(official);
        e.setPostalAddress(postal);
        return e;
    }

    public Organization toOrganization(OrganizationCreateDTO dto, Address official, Address postal) {
        if (dto == null) return null;
        Organization e = new Organization();
        e.setName(dto.getName());
        e.setFullName(dto.getFullName());
        e.setAnnualTurnover(dto.getAnnualTurnover());
        e.setEmployeesCount(dto.getEmployeesCount());
        e.setRating(dto.getRating());
        e.setOfficialAddress(official);
        e.setPostalAddress(postal);
        return e;
    }

    public Person toNullablePerson(PersonImportDTO dto, Location location) {
        if (dto == null) return null;
        Person e = new Person();

        if (dto.getName() == null || dto.getName().trim().isEmpty())
            throw new IllegalArgumentException("person.name must not be blank");

        Double height = asDouble(dto.getHeight());
        if (height == null || height <= 0)
            throw new IllegalArgumentException("person.height must be > 0");

        e.setName(dto.getName());
        e.setLocation(location);
        e.setHeight(height);

        e.setEyeColor(dto.getEyeColor() == null ? null : Color.valueOf(dto.getEyeColor().toUpperCase()));
        e.setHairColor(dto.getHairColor() == null ? null : Color.valueOf(dto.getHairColor().toUpperCase()));
        e.setNationality(dto.getNationality() == null ? null : Country.valueOf(dto.getNationality().toUpperCase()));
        return e;
    }

    public Person toNullablePerson(PersonCreateDTO dto, Location location) {
        if (dto == null) return null;
        Person e = new Person();
        e.setName(dto.getName());
        e.setLocation(location);
        e.setHeight(dto.getHeight());
        e.setEyeColor(dto.getEyeColor());
        e.setHairColor(dto.getHairColor());
        e.setNationality(dto.getNationality());
        return e;
    }

    public Product toProduct(ProductImportDTO dto, Organization org, Person owner, Coordinates coord) {
        if (dto == null) return null;
        Product e = new Product();

        if (dto.getName() == null || dto.getName().trim().isEmpty())
            throw new IllegalArgumentException("product.name must not be blank");
        if (dto.getPartNumber() == null || dto.getPartNumber().trim().isEmpty())
            throw new IllegalArgumentException("product.partNumber must not be blank");
        if (dto.getUnitOfMeasure() == null)
            throw new IllegalArgumentException("product.unitOfMeasure must not be null");

        e.setName(dto.getName());
        e.setCoordinates(coord);
        e.setUnitOfMeasure(UnitOfMeasure.valueOf(dto.getUnitOfMeasure().toUpperCase()));
        e.setManufacturer(org);

        Long price = asLong(dto.getPrice());
        if (price == null || price <= 0)
            throw new IllegalArgumentException("product.price must be > 0");
        e.setPrice(price);

        Integer mcost = asInt(dto.getManufactureCost());
        if (mcost != null) e.setManufactureCost(mcost);

        Long rating = asLong(dto.getRating());
        if (rating == null || rating <= 0)
            throw new IllegalArgumentException("product.rating must be > 0");
        e.setRating(rating);

        e.setPartNumber(dto.getPartNumber());
        e.setOwner(owner);
        return e;
    }
}
