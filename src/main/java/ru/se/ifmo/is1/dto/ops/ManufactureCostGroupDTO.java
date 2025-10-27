package ru.se.ifmo.is1.dto.ops;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ManufactureCostGroupDTO {
    private Number manufactureCost;
    private Long count;
}

