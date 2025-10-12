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
    private CoordinatesCreateDTO coordinates;   // x<=450, y>-422, not null
    private UnitOfMeasure unitOfMeasure;        // not null
    private OrganizationRefDTO manufacturer;    // not null, id required
    private Long price;                         // >0, not null
    private Integer manufactureCost;            // can be null
    private Long rating;                        // >0, not null
    private String partNumber;                  // not empty, not null
    private PersonRefDTO owner;                 // can be null
}
