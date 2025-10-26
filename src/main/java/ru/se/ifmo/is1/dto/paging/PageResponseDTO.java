package ru.se.ifmo.is1.dto.paging;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Data
public class PageResponseDTO<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrev;
    private String sort;
    private String dir;

    public static <T> PageResponseDTO<T> of(List<T> items, int page, int size, long total, String sort, String dir) {
        int totalPages = (int) Math.max(1, (total + size - 1) / size);
        return PageResponseDTO.<T>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .hasPrev(page > 0)
                .sort(sort)
                .dir(dir)
                .build();
    }
}
