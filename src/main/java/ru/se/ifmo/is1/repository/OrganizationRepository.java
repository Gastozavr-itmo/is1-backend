package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Organization;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizationRepository {
    private final SessionFactory sf;
    private Session s() { return sf.getCurrentSession(); }

    public Optional<Organization> findById(Integer id) { return Optional.ofNullable(s().get(Organization.class, id)); }
    public List<Organization> findAll() { return s().createQuery("from Organization", Organization.class).list(); }
    public Integer save(Organization o) { s().persist(o); return o.getId(); }
    public Integer merge(Organization o) { return ((Organization) s().merge(o)).getId(); }
    public void delete(Organization o) { s().remove(o); }
}
