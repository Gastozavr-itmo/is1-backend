package ru.se.ifmo.is1.dto.organization;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrganizationViewDTO {
    Integer id;
    String name;
}
