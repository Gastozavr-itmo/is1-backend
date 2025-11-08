package ru.se.ifmo.is1.dto.organization;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import ru.se.ifmo.is1.dto.shared.AddressDTO;

@Value
@Builder
@Data
public class OrganizationViewDTO {
    private Integer id;
    private String name;
    private AddressDTO officialAddress;
    private Double annualTurnover;
    private Integer employeesCount;
    private String fullName;
    private Integer rating;

    AddressDTO postalAddress;
}
