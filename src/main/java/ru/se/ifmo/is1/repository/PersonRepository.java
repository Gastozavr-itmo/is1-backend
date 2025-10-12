package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Person;
import ru.se.ifmo.is1.repository.util.SortSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PersonRepository {
    private final SessionFactory sf;
    private Session s(){ return sf.getCurrentSession(); }

    public Optional<Person> findById(Long id){ return Optional.ofNullable(s().get(Person.class, id)); }
    public Long save(Person e){ s().persist(e); return e.getId(); }
    public Long merge(Person e){ return ((Person) s().merge(e)).getId(); }
    public void delete(Person e){ s().remove(e); }

    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();
    static {
        SORT.put("id",              SortSupport.Rule.column("p.id"));
        SORT.put("name",            SortSupport.Rule.column("p.name"));
        SORT.put("height",          SortSupport.Rule.column("p.height"));
        SORT.put("nationality",     SortSupport.Rule.column("p.nationality"));
        SORT.put("birthday",        SortSupport.Rule.column("p.birthday"));
        SORT.put("createdAt",       SortSupport.Rule.column("p.created_at"));
        SORT.put("updatedAt",       SortSupport.Rule.column("p.updated_at"));
    }

    public List<Person> findPageNative(int offset, int size, String sort, String dir) {
        var built = SortSupport.build(SORT, sort, dir, "id");
        String sql = "select p.* from person p " +
                "order by " + built.orderBy() +
                " limit :size offset :offset";
        return s().createNativeQuery(sql, Person.class)
                .setParameter("size", size)
                .setParameter("offset", offset)
                .getResultList();
    }

    public long countAllNative() {
        return ((Number) s().createNativeQuery("select count(*) from person").getSingleResult()).longValue();
    }
}
