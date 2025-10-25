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

    private Session s() {
        return sf.getCurrentSession();
    }

    public Optional<Person> findById(Long id) {
        return Optional.ofNullable(s().get(Person.class, id));
    }

    public Long save(Person e) {
        s().persist(e);
        return e.getId();
    }

    public Long merge(Person e) {
        return ((Person) s().merge(e)).getId();
    }

    public void delete(Person e) {
        s().remove(e);
    }

    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();

    static {
        SORT.put("id",          SortSupport.Rule.column("p.id"));
        SORT.put("name",        SortSupport.Rule.column("p.name"));
        SORT.put("height",      SortSupport.Rule.column("p.height"));
        SORT.put("nationality", SortSupport.Rule.column("p.nationality"));
        SORT.put("eyeColor",    SortSupport.Rule.column("p.eyeColor"));
        SORT.put("hairColor",   SortSupport.Rule.column("p.hairColor"));

        // location — встраиваемый value-объект
        SORT.put("locationName", SortSupport.Rule.column("p.location.name"));
        SORT.put("locationX",    SortSupport.Rule.column("p.location.x"));
        SORT.put("locationY",    SortSupport.Rule.column("p.location.y"));
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

    public List<Person> findFiltered(
            String name,
            String eyeColorLike,
            String hairColorLike,
            String nationalityLike,
            String locationName,            // ← добавлено
            int offset, int limit, String sort, String dir
    ) {
        // подготовим множества enum по подстроке
        List<ru.se.ifmo.is1.model.Color> eyes = null;
        List<ru.se.ifmo.is1.model.Color> hairs = null;
        List<ru.se.ifmo.is1.model.Country> nats = null;

        if (eyeColorLike != null && !eyeColorLike.isBlank()) {
            String n = eyeColorLike.trim().toLowerCase();
            eyes = java.util.Arrays.stream(ru.se.ifmo.is1.model.Color.values())
                    .filter(c -> c.name().toLowerCase().contains(n))
                    .toList();
            if (eyes.isEmpty()) return java.util.List.of();
        }
        if (hairColorLike != null && !hairColorLike.isBlank()) {
            String n = hairColorLike.trim().toLowerCase();
            hairs = java.util.Arrays.stream(ru.se.ifmo.is1.model.Color.values())
                    .filter(c -> c.name().toLowerCase().contains(n))
                    .toList();
            if (hairs.isEmpty()) return java.util.List.of();
        }
        if (nationalityLike != null && !nationalityLike.isBlank()) {
            String n = nationalityLike.trim().toLowerCase();
            nats = java.util.Arrays.stream(ru.se.ifmo.is1.model.Country.values())
                    .filter(c -> c.name().toLowerCase().contains(n))
                    .toList();
            if (nats.isEmpty()) return java.util.List.of();
        }

        StringBuilder hql = new StringBuilder("""
            select p
            from Person p
            where 1=1
        """);

        if (name != null && !name.isBlank())        hql.append(" and lower(p.name) like lower(:name) ");
        if (eyes != null)                            hql.append(" and p.eyeColor in (:eyes) ");
        if (hairs != null)                           hql.append(" and p.hairColor in (:hairs) ");
        if (nats != null)                            hql.append(" and p.nationality in (:nats) ");
        if (locationName != null && !locationName.isBlank())
            hql.append(" and lower(p.location.name) like lower(:locName) ");

        var built = SortSupport.build(SORT, sort, dir, "id");
        hql.append(" order by ").append(built.orderBy());

        var q = s().createQuery(hql.toString(), Person.class);
        if (name != null && !name.isBlank())        q.setParameter("name", "%" + name.trim() + "%");
        if (eyes != null)                            q.setParameterList("eyes", eyes);
        if (hairs != null)                           q.setParameterList("hairs", hairs);
        if (nats != null)                            q.setParameterList("nats", nats);
        if (locationName != null && !locationName.isBlank())
            q.setParameter("locName", "%" + locationName.trim() + "%");

        q.setFirstResult(Math.max(offset, 0));
        q.setMaxResults(Math.max(limit, 1));
        return q.list();
    }

    public long countFiltered(
            String name,
            String eyeColorLike,
            String hairColorLike,
            String nationalityLike,
            String locationName            // ← добавлено
    ) {
        List<ru.se.ifmo.is1.model.Color> eyes = null;
        List<ru.se.ifmo.is1.model.Color> hairs = null;
        List<ru.se.ifmo.is1.model.Country> nats = null;

        if (eyeColorLike != null && !eyeColorLike.isBlank()) {
            String n = eyeColorLike.trim().toLowerCase();
            eyes = java.util.Arrays.stream(ru.se.ifmo.is1.model.Color.values())
                    .filter(c -> c.name().toLowerCase().contains(n))
                    .toList();
            if (eyes.isEmpty()) return 0L;
        }
        if (hairColorLike != null && !hairColorLike.isBlank()) {
            String n = hairColorLike.trim().toLowerCase();
            hairs = java.util.Arrays.stream(ru.se.ifmo.is1.model.Color.values())
                    .filter(c -> c.name().toLowerCase().contains(n))
                    .toList();
            if (hairs.isEmpty()) return 0L;
        }
        if (nationalityLike != null && !nationalityLike.isBlank()) {
            String n = nationalityLike.trim().toLowerCase();
            nats = java.util.Arrays.stream(ru.se.ifmo.is1.model.Country.values())
                    .filter(c -> c.name().toLowerCase().contains(n))
                    .toList();
            if (nats.isEmpty()) return 0L;
        }

        StringBuilder hql = new StringBuilder("""
            select count(p.id)
            from Person p
            where 1=1
        """);

        if (name != null && !name.isBlank())        hql.append(" and lower(p.name) like lower(:name) ");
        if (eyes != null)                            hql.append(" and p.eyeColor in (:eyes) ");
        if (hairs != null)                           hql.append(" and p.hairColor in (:hairs) ");
        if (nats != null)                            hql.append(" and p.nationality in (:nats) ");
        if (locationName != null && !locationName.isBlank())
            hql.append(" and lower(p.location.name) like lower(:locName) ");

        var q = s().createQuery(hql.toString(), Long.class);
        if (name != null && !name.isBlank())        q.setParameter("name", "%" + name.trim() + "%");
        if (eyes != null)                            q.setParameterList("eyes", eyes);
        if (hairs != null)                           q.setParameterList("hairs", hairs);
        if (nats != null)                            q.setParameterList("nats", nats);
        if (locationName != null && !locationName.isBlank())
            q.setParameter("locName", "%" + locationName.trim() + "%");

        return q.getSingleResult();
    }
}
