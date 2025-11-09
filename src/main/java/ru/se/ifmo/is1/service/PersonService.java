package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.concurrency.KeyLock;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.mapper.PersonMapper;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Person;
import ru.se.ifmo.is1.repository.PersonRepository;
import ru.se.ifmo.is1.ws.ChangePublisher;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repo;
    private final PersonMapper mapper;
    private final ChangePublisher changes;

    @Transactional(readOnly = true)
    public PersonViewDTO get(Long id) {
        return mapper.toView(
                repo.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Person not found"))
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<PersonViewDTO> list(
            int page, int size, String sort, String dir,
            String name, String eyeColorLike, String hairColorLike, String nationalityLike,
            String locationName
    ) {
        int offset = Math.max(page, 0) * Math.max(size, 1);

        var rows = repo.findFiltered(
                name, eyeColorLike, hairColorLike, nationalityLike, locationName,
                offset, size, sort, dir
        );
        long total = repo.countFiltered(
                name, eyeColorLike, hairColorLike, nationalityLike, locationName
        );

        var items = rows.stream()
                .map(mapper::toView)
                .toList();

        return PageResponseDTO.of(items, page, size, total, sort, dir);
    }


    @KeyLock(
            "'person:uniq:' + T(ru.se.ifmo.is1.concurrency.LockKeys).personKey(" +
                    "#p0.name, " +
                    "#p0.nationality, " +
                    "(#p0.location != null ? #p0.location.name : null)" +
                    ")"
    )


    @Transactional
    public Long create(PersonCreateDTO dto) {
        Person p = mapper.toEntity(dto);
        validate(p);
        Long id = repo.save(p);
        changes.broadcast("person", "created", id);
        return id;
    }

    @KeyLock(
            "{ " +
                    // сериализация всех апдейтов одного и того же Person
                    "'person:id:' + #p0, " +
                    // защита от гонки при смене бизнес-ключа
                    "'person:uniq:' + T(ru.se.ifmo.is1.concurrency.LockKeys).personKey(" +
                    "#p1.name, #p1.nationality, (#p1.location != null ? #p1.location.name : null)" +
                    ")" +
                    " }"
    )

    @Transactional
    public void update(Long id, PersonCreateDTO dto) {
        Person p = mapper.toEntity(dto);
        p.setId(id);
        validate(p);
        repo.merge(p);
        changes.broadcast("person", "updated", id);
    }

    @KeyLock("'person:id:' + #p0")
    @Transactional
    public void delete(Long id) {
        var e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found"));
        repo.delete(e);
        changes.broadcast("person", "deleted", id);
    }


    private void validate(Person p) {
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name required");
        }
        if (p.getHeight() <= 0) {
            throw new IllegalArgumentException("height > 0 required");
        }
        if (p.getNationality() == null) {
            throw new IllegalArgumentException("nationality required");
        }
        Location l = p.getLocation();
        if (l != null) {
            if (l.getX() == null || l.getY() == null) {
                throw new IllegalArgumentException("location x,y required if location present");
            }
            if (l.getName() == null || l.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("location.name required");
            }
        }
    }
}
