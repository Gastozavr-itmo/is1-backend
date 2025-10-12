package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Person;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PersonRepository {
    private final SessionFactory sf;
    private Session s() { return sf.getCurrentSession(); }

    public Optional<Person> findById(Long id) { return Optional.ofNullable(s().get(Person.class, id)); }
    public List<Person> findAll() { return s().createQuery("from Person", Person.class).list(); }
    public Long save(Person p) { s().persist(p); return p.getId(); }
    public Long merge(Person p) { return ((Person) s().merge(p)).getId(); }
    public void delete(Person p) { s().remove(p); }
}
