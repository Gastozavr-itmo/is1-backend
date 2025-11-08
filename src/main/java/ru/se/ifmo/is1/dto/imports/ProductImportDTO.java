package ru.se.ifmo.is1.dto.imports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import ru.se.ifmo.is1.dto.shared.CoordinatesDTO;

@Data
public class ProductImportDTO {
    @NotBlank
    private String name;

    @Valid @NotNull
    private CoordinatesDTO coordinates;

    @NotNull
    private String unitOfMeasure; // конвертируем к enum в маппере

    @Valid @NotNull
    private OrganizationImportDTO manufacturer;

    @Positive
    private Integer price;

    @Positive
    private Integer manufactureCost;

    @Positive
    private Integer rating;

    @NotBlank
    private String partNumber;

    @Valid
    private PersonImportDTO owner; // может быть null
}
