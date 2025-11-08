package ru.se.ifmo.is1.dto.shared;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinatesDTO {
    private Double x;
    private Long y;
}
