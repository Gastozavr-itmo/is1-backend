package ru.se.ifmo.is1.dto.organization;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import ru.se.ifmo.is1.dto.shared.AddressCreateDTO;

@Value
@Builder
@Data
public class OrganizationViewDTO {
    private Integer id;
    private String name;
    private AddressCreateDTO officialAddress;
    private Double annualTurnover;
    private Integer employeesCount;
    private String fullName;
    private Integer rating;

    AddressCreateDTO postalAddress;
}
