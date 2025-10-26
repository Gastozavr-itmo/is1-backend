package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.organization.OrganizationCreateDTO;
import ru.se.ifmo.is1.dto.organization.OrganizationViewDTO;
import ru.se.ifmo.is1.dto.paging.PageRequestDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.service.OrganizationService;


@RestController
@RequestMapping("/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

    @GetMapping("/{id}")
    public OrganizationViewDTO get(@PathVariable("id") Integer id) {
        return service.get(id);
    }

    @GetMapping
    public ResponseEntity<
            ru.se.ifmo.is1.dto.paging.PageResponseDTO<ru.se.ifmo.is1.dto.organization.OrganizationViewDTO>
            > list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "dir",  defaultValue = "asc") String dir,

            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "officialTownName", required = false) String officialTownName,
            @RequestParam(value = "postalTownName", required = false) String postalTownName
    ) {
        var resp = service.list(page, size, sort, dir, name, fullName, officialTownName, postalTownName);
        return org.springframework.http.ResponseEntity.ok(resp);
    }



    @PostMapping
    public ResponseEntity<Integer> create(@RequestBody OrganizationCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Integer id, @RequestBody OrganizationCreateDTO dto) {
        service.update(id, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
