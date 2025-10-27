package ru.se.ifmo.is1.dto.paging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Data
@AllArgsConstructor
public class PageRequestDTO {
    private int page;
    private int size;
    private String sort;
    private String dir;
}