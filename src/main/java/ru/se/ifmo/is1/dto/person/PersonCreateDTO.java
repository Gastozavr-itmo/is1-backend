package ru.se.ifmo.is1.dto.person;

import lombok.*;
import ru.se.ifmo.is1.dto.shared.LocationCreateDTO;
import ru.se.ifmo.is1.model.Color;
import ru.se.ifmo.is1.model.Country;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonCreateDTO {
    private String name;           // not empty
    private Color eyeColor;        // nullable
    private Color hairColor;       // nullable
    private LocationCreateDTO location; // nullable
    private Double height;         // >0
    private Country nationality;   // not null
}
