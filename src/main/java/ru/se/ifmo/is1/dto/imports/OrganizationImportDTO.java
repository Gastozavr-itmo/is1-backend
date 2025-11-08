package ru.se.ifmo.is1.dto.imports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import ru.se.ifmo.is1.dto.shared.*;

@Data
public class OrganizationImportDTO {
    @NotBlank
    private String name;

    private String fullName;

    @Positive
    private Double annualTurnover;

    @Positive
    private Long employeesCount;

    @Positive
    private Integer rating;

    @Valid @NotNull
    private AddressDTO officialAddress;

    @Valid @NotNull
    private AddressDTO postalAddress;
}
