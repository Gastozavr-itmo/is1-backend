package ru.se.ifmo.is1.dto.paging;

import lombok.Value;

@Value
public class PageRequestDTO {
    int page;
    int size;
    String sort;
    String dir;

    public int offset() { return Math.max(0, page) * Math.max(1, size); }

    public String direction() {
        return "desc".equalsIgnoreCase(dir) ? "desc" : "asc";
    }
}