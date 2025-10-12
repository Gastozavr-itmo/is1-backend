package ru.se.ifmo.is1.mapper;

import org.springframework.stereotype.Component;
import ru.se.ifmo.is1.dto.product.*;
import ru.se.ifmo.is1.dto.shared.*;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.model.*;

import java.util.Date;

@Component
public class ProductMapper {

    public Product toEntity(ProductCreateDTO dto) {
        if (dto == null) return null;

        Product p = new Product();
        p.setName(dto.getName());
        if (dto.getCoordinates() != null) {
            CoordinatesCreateDTO cd = dto.getCoordinates();
            Coordinates coords = new Coordinates();
            coords.setX(cd.getX());
            coords.setY(cd.getY());
            p.setCoordinates(coords);
        }
        p.setUnitOfMeasure(dto.getUnitOfMeasure());

        if (dto.getManufacturer() != null) {
            Organization o = new Organization();
            o.setId(dto.getManufacturer().getId());
            p.setManufacturer(o);
        }

        p.setPrice(dto.getPrice());
        p.setManufactureCost(dto.getManufactureCost());
        p.setRating(dto.getRating());
        p.setPartNumber(dto.getPartNumber());
        p.setCreationDate(new Date());

        if (dto.getOwner() != null) {
            Person owner = new Person();
            owner.setId(dto.getOwner().getId());
            p.setOwner(owner);
        }
        return p;
    }

    public ProductViewDTO toView(Product p) {
        return ProductViewDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .coordinates(CoordinatesViewDTO.builder()
                        .x(p.getCoordinates().getX())
                        .y(p.getCoordinates().getY())
                        .build())
                .creationDate(p.getCreationDate())
                .unitOfMeasure(p.getUnitOfMeasure())
                .manufacturer(OrganizationViewDTO.builder()
                        .id(p.getManufacturer().getId())
                        .name(p.getManufacturer().getName())
                        .build())
                .price(p.getPrice())
                .manufactureCost(p.getManufactureCost())
                .rating(p.getRating())
                .partNumber(p.getPartNumber())
                .owner(p.getOwner() != null
                        ? PersonViewDTO.builder().id(p.getOwner().getId()).name(p.getOwner().getName()).build()
                        : null)
                .build();
    }
}
