package ru.se.ifmo.is1.dto.shared;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDTO {
    private Long x;
    private Long y;
    private String name;
}
