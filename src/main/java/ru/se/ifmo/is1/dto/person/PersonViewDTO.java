package ru.se.ifmo.is1.dto.person;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonViewDTO {
    Long id;
    String name;
}
