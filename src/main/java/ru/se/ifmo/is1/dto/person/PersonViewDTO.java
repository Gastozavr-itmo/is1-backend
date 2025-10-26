package ru.se.ifmo.is1.dto.person;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import ru.se.ifmo.is1.model.Color;
import ru.se.ifmo.is1.model.Country;

@Value
@Builder
@Data
public class PersonViewDTO {
    private Long id;
    private String name;
    private Color eyeColor;
    private Color hairColor;

    private String locationName;
    private Long locationX;
    private Long locationY;

    private Double height;
    private Country nationality;
}
