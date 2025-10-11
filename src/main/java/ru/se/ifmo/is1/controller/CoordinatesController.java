package ru.se.ifmo.is1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.se.ifmo.is1.dto.coordinates.CoordinatesCreateDTO;
import ru.se.ifmo.is1.dto.coordinates.CoordinatesViewDTO;
import ru.se.ifmo.is1.service.CoordinatesService;

@RestController
@RequestMapping("/coordinates")
public class CoordinatesController {

    private final CoordinatesService service;
    public CoordinatesController(CoordinatesService service) { this.service = service; }

    // GET /api/coordinates/{id}
    @GetMapping("/{id}")
    public CoordinatesViewDTO get(@PathVariable("id") Long id) {
        return service.get(id);
    }


    // POST /api/coordinates
    @PostMapping
    public ResponseEntity<Long> create(@RequestBody CoordinatesCreateDTO dto) {
        Long id = service.create(dto);
        return ResponseEntity.ok(id);
    }
}
