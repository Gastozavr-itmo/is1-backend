package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonViewFullDTO;
import ru.se.ifmo.is1.mapper.PersonMapper;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Person;
import ru.se.ifmo.is1.repository.PersonRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository repo;
    private final PersonMapper mapper;

    @Transactional(readOnly = true)
    public PersonViewFullDTO get(Long id) {
        return mapper.toView(repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Person not found")));
    }

    @Transactional(readOnly = true)
    public List<PersonViewFullDTO> list() {
        return repo.findAll().stream().map(mapper::toView).toList();
    }

    @Transactional
    public Long create(PersonCreateDTO dto) {
        Person p = mapper.toEntity(dto);
        validate(p);
        return repo.save(p);
    }

    @Transactional
    public void update(Long id, PersonCreateDTO dto) {
        Person p = mapper.toEntity(dto);
        p.setId(id);
        validate(p);
        repo.merge(p);
    }

    @Transactional
    public void delete(Long id) {
        var e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Person not found"));
        repo.delete(e);
    }

    private void validate(Person p) {
        if (p.getName() == null || p.getName().trim().isEmpty())
            throw new IllegalArgumentException("name required");
        if (p.getHeight() <= 0)
            throw new IllegalArgumentException("height > 0 required");
        if (p.getNationality() == null)
            throw new IllegalArgumentException("nationality required");
        Location l = p.getLocation();
        if (l != null) {
            if (l.getX() == null || l.getY() == null)
                throw new IllegalArgumentException("location x,y required if location present");
            if (l.getName() == null || l.getName().trim().isEmpty())
                throw new IllegalArgumentException("location.name required");
        }
    }
}
