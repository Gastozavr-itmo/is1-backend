package ru.se.ifmo.is1.dto.paging;

import lombok.Data;
import lombok.Value;

@Value
@Data
public class PageRequestDTO {
    private int page;
    private int size;
    private String sort;
    private String dir;
}