package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.imports.ImportOperationDTO;
import ru.se.ifmo.is1.service.ImportHistoryService;

import java.util.List;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportHistoryController {
    private final ImportHistoryService history;

    @GetMapping
    public List<ImportOperationDTO> list() {
        return history.list().stream().map(ImportOperationDTO::from).toList();
    }

    @GetMapping("/{id}")
    public ImportOperationDTO one(@PathVariable Long id) {
        return ImportOperationDTO.from(history.find(id));
    }
}
