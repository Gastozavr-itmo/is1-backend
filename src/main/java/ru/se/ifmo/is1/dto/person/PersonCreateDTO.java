package ru.se.ifmo.is1.dto.person;

import lombok.*;
import ru.se.ifmo.is1.dto.shared.LocationDTO;
import ru.se.ifmo.is1.model.Color;
import ru.se.ifmo.is1.model.Country;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonCreateDTO {
    private String name;
    private Color eyeColor;
    private Color hairColor;
    private LocationDTO location;
    private Double height;
    private Country nationality;
}
