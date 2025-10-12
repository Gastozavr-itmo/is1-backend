package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.paging.PageRequestDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.dto.product.ProductCreateDTO;
import ru.se.ifmo.is1.dto.product.ProductViewDTO;
import ru.se.ifmo.is1.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping("/{id}")
    public ProductViewDTO get(@PathVariable("id") Long id) {
        return service.get(id);
    }



    @GetMapping
    public ResponseEntity<PageResponseDTO<ru.se.ifmo.is1.dto.product.ProductViewDTO>> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "dir",  defaultValue = "asc") String dir,

            // ФИЛЬТРЫ
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "partNumber", required = false) String partNumber,
            @RequestParam(value = "unit", required = false) String unitOfMeasureLike, // ВАЖНО: строка
            @RequestParam(value = "organizationName", required = false) String organizationName,
            @RequestParam(value = "personName", required = false) String personName
    ) {
        var pageResp = service.list(page, size, sort, dir, name, partNumber, unitOfMeasureLike, organizationName, personName);
        return ResponseEntity.ok(pageResp);
    }





    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ProductCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id, @RequestBody ProductCreateDTO dto) {
        service.update(id, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
