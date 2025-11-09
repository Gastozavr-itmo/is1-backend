package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.imports.ImportOperationDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.service.ImportHistoryService;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportHistoryController {
    private final ImportHistoryService history;

    @GetMapping
    public PageResponseDTO<ImportOperationDTO> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "dir", defaultValue = "desc") String dir
    ) {
        return history.list(page, size, sort, dir);
    }

    @GetMapping("/{id}")
    public ImportOperationDTO one(@PathVariable Long id) {
        return ImportOperationDTO.from(history.find(id));
    }
}
