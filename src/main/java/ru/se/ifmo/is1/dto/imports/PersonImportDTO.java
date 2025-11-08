package ru.se.ifmo.is1.dto.imports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import ru.se.ifmo.is1.dto.shared.LocationDTO;

@Data
public class PersonImportDTO {
    @NotBlank
    private String name;

    @NotNull
    private String nationality; // enum в домене

    @NotNull
    private String eyeColor; // enum

    @NotNull
    private String hairColor; // enum

    @Positive
    private Double height;

    @Valid
    private LocationDTO location; // может быть null по твоей модели
}
