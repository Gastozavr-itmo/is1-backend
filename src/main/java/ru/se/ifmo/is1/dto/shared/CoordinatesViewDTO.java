package ru.se.ifmo.is1.dto.shared;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CoordinatesViewDTO {
    Double x;
    Long y;
}
