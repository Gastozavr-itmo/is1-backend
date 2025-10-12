package ru.se.ifmo.is1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonViewFullDTO;
import ru.se.ifmo.is1.service.PersonService;

import java.util.List;

@RestController
@RequestMapping("/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @GetMapping("/{id}")
    public PersonViewFullDTO get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<PersonViewFullDTO> list() {
        return service.list();
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
