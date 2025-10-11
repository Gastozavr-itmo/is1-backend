package ru.se.ifmo.is1.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.model.Coordinates;
import ru.se.ifmo.is1.dto.coordinates.CoordinatesCreateDTO;
import ru.se.ifmo.is1.dto.coordinates.CoordinatesViewDTO;
import ru.se.ifmo.is1.repository.CoordinatesRepository;

@Service
public class CoordinatesService {

    private final CoordinatesRepository repo;
    public CoordinatesService(CoordinatesRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public CoordinatesViewDTO get(Long id) {
        var c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Coordinates not found"));
        return new CoordinatesViewDTO(c.getId(), c.getX(), c.getY());
    }

    @Transactional
    public Long create(CoordinatesCreateDTO dto) {
        if (dto.x() == null) throw new IllegalArgumentException("x must not be null");
        if (dto.y() == null) throw new IllegalArgumentException("y must not be null");
        if (dto.x() > 450.0) throw new IllegalArgumentException("x must be <= 450");
        if (dto.y() <= -422) throw new IllegalArgumentException("y must be > -422");

        Coordinates c = new Coordinates();
        c.setX(dto.x());
        c.setY(dto.y());
        return repo.save(c);
    }
}
