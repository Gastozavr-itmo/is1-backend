package ru.se.ifmo.is1.dto.shared;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinatesCreateDTO {
    private Double x;
    private Long y;
}
