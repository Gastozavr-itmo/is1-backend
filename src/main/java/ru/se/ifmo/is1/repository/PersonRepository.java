package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Color;
import ru.se.ifmo.is1.model.Country;
import ru.se.ifmo.is1.model.Person;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.repository.util.SortSupport;

import java.util.*;

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
        ensureUniqueBusinessKey(e, null); // при INSERT selfId ещё нет
        s().persist(e);
        return e.getId();
    }

    public Long merge(Person e) {
        ensureUniqueBusinessKey(e, e.getId()); // при UPDATE игнорируем сами себя
        return ((Person) s().merge(e)).getId();
    }

    public void delete(Person e) {
        s().remove(e);
    }

    public Person findByBusinessKey(
            String nameLowerNorm,
            Country nationality,
            Long locX, Long locY,
            String locNameLowerNorm
    ) {
        String nm = nameLowerNorm == null ? null : nameLowerNorm.trim().toLowerCase();
        String ln = locNameLowerNorm == null ? null : locNameLowerNorm.trim().toLowerCase();

        return s().createQuery("""
                        select p from Person p
                        where lower(p.name) = :nm
                          and p.nationality = :nat
                          and ((p.location.x is null and :lx is null) or p.location.x = :lx)
                          and ((p.location.y is null and :ly is null) or p.location.y = :ly)
                          and ((p.location.name is null and :ln is null) or lower(p.location.name) = :ln)
                        """, Person.class)
                .setParameter("nm", nm)
                .setParameter("nat", nationality)
                .setParameter("lx", locX)
                .setParameter("ly", locY)
                .setParameter("ln", ln)
                .setMaxResults(1)
                .uniqueResult();
    }


    public Person findByBusinessKey(Person p) {
        if (p == null) return null;

        Location loc = p.getLocation();
        Long locX = loc != null ? loc.getX() : null;
        Long locY = loc != null ? loc.getY() : null;
        String locName = loc != null ? loc.getName() : null;

        return findByBusinessKey(
                p.getName(),
                p.getNationality(),
                locX, locY,
                locName
        );
    }


    private void ensureUniqueBusinessKey(Person e, Long selfId) {
        if (e == null) return;

        Person existing = findByBusinessKey(e);
        if (existing != null && (selfId == null || existing.getId() != selfId)) {
            throw new IllegalArgumentException(
                    "Person with same name, nationality and address already exists"
            );
        }
    }

    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();

    static {
        SORT.put("id", SortSupport.Rule.column("p.id"));
        SORT.put("name", SortSupport.Rule.column("p.name"));
        SORT.put("height", SortSupport.Rule.column("p.height"));
        SORT.put("nationality", SortSupport.Rule.column("p.nationality"));
        SORT.put("eyeColor", SortSupport.Rule.column("p.eyeColor"));
        SORT.put("hairColor", SortSupport.Rule.column("p.hairColor"));
        SORT.put("locationName", SortSupport.Rule.column("p.location.name"));
        SORT.put("locationX", SortSupport.Rule.column("p.location.x"));
        SORT.put("locationY", SortSupport.Rule.column("p.location.y"));
    }

    public List<Person> findFiltered(
            String name, String eyeColorLike, String hairColorLike, String nationalityLike, String locationName,
            int offset, int limit, String sort, String dir
    ) {
        List<Color> eyes = null;
        List<Color> hairs = null;
        List<Country> nats = null;

        if (eyeColorLike != null && !eyeColorLike.isBlank()) {
            String n = eyeColorLike.trim().toLowerCase();
            eyes = Arrays.stream(Color.values()).filter(c -> c.name().toLowerCase().contains(n)).toList();
            if (eyes.isEmpty()) return List.of();
        }
        if (hairColorLike != null && !hairColorLike.isBlank()) {
            String n = hairColorLike.trim().toLowerCase();
            hairs = Arrays.stream(Color.values()).filter(c -> c.name().toLowerCase().contains(n)).toList();
            if (hairs.isEmpty()) return List.of();
        }
        if (nationalityLike != null && !nationalityLike.isBlank()) {
            String n = nationalityLike.trim().toLowerCase();
            nats = Arrays.stream(Country.values()).filter(c -> c.name().toLowerCase().contains(n)).toList();
            if (nats.isEmpty()) return List.of();
        }

        StringBuilder hql = new StringBuilder("""
                    select p from Person p where 1=1
                """);
        if (name != null && !name.isBlank()) hql.append(" and lower(p.name) like lower(:name) ");
        if (eyes != null) hql.append(" and p.eyeColor in (:eyes) ");
        if (hairs != null) hql.append(" and p.hairColor in (:hairs) ");
        if (nats != null) hql.append(" and p.nationality in (:nats) ");
        if (locationName != null && !locationName.isBlank())
            hql.append(" and lower(p.location.name) like lower(:locName) ");

        var built = SortSupport.build(SORT, sort, dir, "id");
        hql.append(" order by ").append(built.orderBy());

        var q = s().createQuery(hql.toString(), Person.class);
        if (name != null && !name.isBlank()) q.setParameter("name", "%" + name.trim() + "%");
        if (eyes != null) q.setParameterList("eyes", eyes);
        if (hairs != null) q.setParameterList("hairs", hairs);
        if (nats != null) q.setParameterList("nats", nats);
        if (locationName != null && !locationName.isBlank())
            q.setParameter("locName", "%" + locationName.trim() + "%");

        q.setFirstResult(Math.max(offset, 0));
        q.setMaxResults(Math.max(limit, 1));
        return q.list();
    }

    public long countFiltered(
            String name, String eyeColorLike, String hairColorLike, String nationalityLike, String locationName
    ) {
        List<Color> eyes = null;
        List<Color> hairs = null;
        List<Country> nats = null;

        if (eyeColorLike != null && !eyeColorLike.isBlank()) {
            String n = eyeColorLike.trim().toLowerCase();
            eyes = Arrays.stream(Color.values()).filter(c -> c.name().toLowerCase().contains(n)).toList();
            if (eyes.isEmpty()) return 0L;
        }
        if (hairColorLike != null && !hairColorLike.isBlank()) {
            String n = hairColorLike.trim().toLowerCase();
            hairs = Arrays.stream(Color.values()).filter(c -> c.name().toLowerCase().contains(n)).toList();
            if (hairs.isEmpty()) return 0L;
        }
        if (nationalityLike != null && !nationalityLike.isBlank()) {
            String n = nationalityLike.trim().toLowerCase();
            nats = Arrays.stream(Country.values()).filter(c -> c.name().toLowerCase().contains(n)).toList();
            if (nats.isEmpty()) return 0L;
        }

        StringBuilder hql = new StringBuilder("""
                    select count(p.id) from Person p where 1=1
                """);
        if (name != null && !name.isBlank()) hql.append(" and lower(p.name) like lower(:name) ");
        if (eyes != null) hql.append(" and p.eyeColor in (:eyes) ");
        if (hairs != null) hql.append(" and p.hairColor in (:hairs) ");
        if (nats != null) hql.append(" and p.nationality in (:nats) ");
        if (locationName != null && !locationName.isBlank())
            hql.append(" and lower(p.location.name) like lower(:locName) ");

        var q = s().createQuery(hql.toString(), Long.class);
        if (name != null && !name.isBlank()) q.setParameter("name", "%" + name.trim() + "%");
        if (eyes != null) q.setParameterList("eyes", eyes);
        if (hairs != null) q.setParameterList("hairs", hairs);
        if (nats != null) q.setParameterList("nats", nats);
        if (locationName != null && !locationName.isBlank())
            q.setParameter("locName", "%" + locationName.trim() + "%");

        return q.getSingleResult();
    }
}
