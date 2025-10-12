package ru.se.ifmo.is1.dto.product;

import lombok.Builder;
import lombok.Value;
import ru.se.ifmo.is1.dto.shared.CoordinatesViewDTO;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.model.UnitOfMeasure;

import java.util.Date;

@Value
@Builder
public class ProductViewDTO {
    Long id;
    String name;
    CoordinatesViewDTO coordinates;
    Date creationDate;
    UnitOfMeasure unitOfMeasure;
    OrganizationViewDTO manufacturer;
    Long price;
    Integer manufactureCost;
    Long rating;
    String partNumber;
    PersonViewDTO owner; // nullable
}
