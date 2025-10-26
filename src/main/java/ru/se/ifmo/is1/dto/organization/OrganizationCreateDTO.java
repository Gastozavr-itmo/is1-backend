package ru.se.ifmo.is1.dto.organization;

import lombok.*;
import ru.se.ifmo.is1.dto.shared.AddressCreateDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationCreateDTO {
    private String name;
    private Double annualTurnover;
    private Integer employeesCount;
    private String fullName;
    private Integer rating;
    private AddressCreateDTO officialAddress;
    private AddressCreateDTO postalAddress;
}
