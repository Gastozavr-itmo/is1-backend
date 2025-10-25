package ru.se.ifmo.is1.dto.person;

import lombok.Builder;
import lombok.Value;
import ru.se.ifmo.is1.model.Color;
import ru.se.ifmo.is1.model.Country;

@Value
@Builder
public class PersonViewDTO {
    Long id;
    String name;
    Color eyeColor;
    Color hairColor;

    String locationName;
    Long locationX;
    Long locationY;

    Double height;
    Country nationality;
}
