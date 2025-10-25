package ru.se.ifmo.is1.dto.organization;

import lombok.Builder;
import lombok.Value;
import ru.se.ifmo.is1.dto.shared.AddressCreateDTO; // используем имеющиеся holder’ы

@Value
@Builder
public class OrganizationViewDTO {
    Integer id;
    String name;
    AddressCreateDTO officialAddress;
    Double annualTurnover;
    Integer employeesCount;
    String fullName;
    Integer rating;

    AddressCreateDTO postalAddress;
}
