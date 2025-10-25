package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.service.PersonService;

@RestController
@RequestMapping("/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @GetMapping("/{id}")
    public PersonViewDTO get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ru.se.ifmo.is1.dto.person.PersonViewDTO>> list(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "dir",  defaultValue = "asc") String dir,

            @RequestParam(value = "name",        required = false) String name,
            @RequestParam(value = "eyeColor",    required = false) String eyeColorLike,
            @RequestParam(value = "hairColor",   required = false) String hairColorLike,
            @RequestParam(value = "nationality", required = false) String nationalityLike,
            @RequestParam(value = "locationName", required = false) String locationName
    ) {
        var pageResp = service.list(page, size, sort, dir,
                name, eyeColorLike, hairColorLike, nationalityLike, locationName);
        return ResponseEntity.ok(pageResp);
    }





    @PostMapping
    public ResponseEntity<Long> create(@RequestBody PersonCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable("id") Long id, @RequestBody PersonCreateDTO dto) {
        service.update(id, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
