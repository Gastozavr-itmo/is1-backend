package ru.se.ifmo.is1.dto.product;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.dto.shared.CoordinatesCreateDTO;
import ru.se.ifmo.is1.model.UnitOfMeasure;

import java.util.Date;

@Value
@Builder
@Data
public class ProductViewDTO {
    private Long id;
    private String name;
    private CoordinatesCreateDTO coordinates;
    private Date creationDate;
    private UnitOfMeasure unitOfMeasure;
    private OrganizationViewDTO manufacturer;
    private Long price;
    private Integer manufactureCost;
    private Long rating;
    private String partNumber;
    private PersonViewDTO owner;
}
