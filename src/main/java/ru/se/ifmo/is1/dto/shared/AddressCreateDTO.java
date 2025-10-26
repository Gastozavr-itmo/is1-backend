package ru.se.ifmo.is1.dto.shared;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressCreateDTO {
    private String zipCode;
    private TownCreateDTO town;
}
