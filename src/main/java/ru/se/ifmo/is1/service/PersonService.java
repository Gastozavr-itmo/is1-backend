package ru.se.ifmo.is1.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.mapper.PersonMapper;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Person;
import ru.se.ifmo.is1.repository.PersonRepository;
import ru.se.ifmo.is1.ws.ChangePublisher;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository repo;
    private final PersonMapper mapper;
    private final ChangePublisher changes;

    @Transactional(readOnly = true)
    public PersonViewDTO get(Long id) {
        return mapper.toView(repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Person not found")));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<PersonViewDTO> list(
            int page, int size, String sort, String dir,
            String name, String eyeColorLike, String hairColorLike, String nationalityLike,
            String locationName) {
        int offset = Math.max(page, 0) * Math.max(size, 1);

        var rows  = repo.findFiltered(name, eyeColorLike, hairColorLike, nationalityLike,locationName,
                offset, size, sort, dir);
        long total = repo.countFiltered(name, eyeColorLike, hairColorLike, nationalityLike,locationName);

        var items = rows.stream().map(mapper::toView).toList(); // ВАЖНО: "лёгкий" DTO
        return PageResponseDTO.of(items, page, size, total, sort, dir);
    }

    @Transactional(isolation = SERIALIZABLE)
    public Long create(PersonCreateDTO dto) {
        Person p = mapper.toEntity(dto);
        validate(p);
        String nameLowerNorm = p.getName() == null ? null : p.getName().trim().toLowerCase();

        Person existing = repo.findByBusinessKey(nameLowerNorm);

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Person with same business key already exists (id=" + existing.getId() + ")"
            );
        }

        Long id = repo.save(p);
        changes.broadcast("person", "created", id);
        return id;
    }

    @Transactional(isolation = SERIALIZABLE)
    public void update(Long id, PersonCreateDTO dto) {
        Person p = mapper.toEntity(dto);
        p.setId(id);
        validate(p);
        repo.merge(p);
        changes.broadcast("person","updated", id);
    }


    @Transactional(isolation = SERIALIZABLE)
    public void delete(Long id) {
        var e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Person not found"));
        repo.delete(e);
        changes.broadcast("person","deleted", id);
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
