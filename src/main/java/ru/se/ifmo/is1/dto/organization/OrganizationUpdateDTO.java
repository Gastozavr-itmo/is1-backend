package ru.se.ifmo.is1.dto.organization;

import lombok.*;
import ru.se.ifmo.is1.dto.shared.AddressCreateDTO;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationUpdateDTO {
    private String name;                        // not null, not blank
    private AddressCreateDTO officialAddress;   // not null
    private Double annualTurnover;              // not null, > 0
    private Integer employeesCount;             // > 0
    private String fullName;                    // nullable, <= 1950
    private Integer rating;                     // > 0
    private AddressCreateDTO postalAddress;     // not null
}
