package ru.se.ifmo.is1.dto.organization;

import lombok.*;
import ru.se.ifmo.is1.dto.shared.AddressCreateDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationCreateDTO {
    private String name;                  // not empty
    private Double annualTurnover;        // >0
    private Integer employeesCount;       // >0
    private String fullName;              // nullable
    private Integer rating;               // >0
    private AddressCreateDTO officialAddress; // not null
    private AddressCreateDTO postalAddress;   // not null
}
