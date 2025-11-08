package ru.se.ifmo.is1.dto.shared;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private String zipCode;
    private LocationDTO town;
}
