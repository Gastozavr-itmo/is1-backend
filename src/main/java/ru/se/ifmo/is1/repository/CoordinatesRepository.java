package ru.se.ifmo.is1.repository;

import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Coordinates;

import java.util.Optional;

@Repository
public class CoordinatesRepository {

    private final SessionFactory sf;
    public CoordinatesRepository(SessionFactory sf) { this.sf = sf; }

    public Optional<Coordinates> findById(Long id) {
        return Optional.ofNullable(sf.getCurrentSession().get(Coordinates.class, id));
    }

    public Long save(Coordinates c) {
        sf.getCurrentSession().persist(c);
        return c.getId();
    }
}
