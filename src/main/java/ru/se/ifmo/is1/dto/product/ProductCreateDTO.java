package ru.se.ifmo.is1.dto.product;

import lombok.*;
import ru.se.ifmo.is1.dto.shared.CoordinatesCreateDTO;
import ru.se.ifmo.is1.dto.shared.OrganizationRefDTO;
import ru.se.ifmo.is1.dto.shared.PersonRefDTO;
import ru.se.ifmo.is1.model.UnitOfMeasure;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateDTO {
    private String name;
    private CoordinatesCreateDTO coordinates;
    private UnitOfMeasure unitOfMeasure;
    private OrganizationRefDTO manufacturer;
    private Long price;
    private Integer manufactureCost;
    private Long rating;
    private String partNumber;
    private PersonRefDTO owner;
}
