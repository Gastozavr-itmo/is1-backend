package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Product;
import ru.se.ifmo.is1.repository.util.SortSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepository {
    private final SessionFactory sf;

    private Session s() {
        return sf.getCurrentSession();
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(s().get(Product.class, id));
    }

    public Long save(Product e) {
        s().persist(e);
        return e.getId();
    }

    public Long merge(Product e) {
        return ((Product) s().merge(e)).getId();
    }

    public void delete(Product e) {
        s().remove(e);
    }

    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();

    static {
        SORT.put("id",            SortSupport.Rule.column("p.id"));
        SORT.put("name",          SortSupport.Rule.column("p.name"));
        SORT.put("price",         SortSupport.Rule.column("p.price"));
        SORT.put("rating",        SortSupport.Rule.column("p.rating"));
        SORT.put("partNumber",    SortSupport.Rule.column("p.partNumber"));
        SORT.put("unitOfMeasure", SortSupport.Rule.column("p.unitOfMeasure"));
        SORT.put("creationDate",  SortSupport.Rule.column("p.creationDate"));

        SORT.put("owner", SortSupport.Rule.column("p.owner.name"));
        SORT.put("manufacturer", SortSupport.Rule.column("p.manufacturer.name"));


    }




    public List<Product> findPageNative(int offset, int size, String sort, String dir) {
        var built = SortSupport.build(SORT, sort, dir, "id");
        String joins = String.join(" ", built.joins());
        String sql = "select p.* from product p " + joins +
                " order by " + built.orderBy() +
                " limit :size offset :offset";
        return s().createNativeQuery(sql, Product.class)
                .setParameter("size", size)
                .setParameter("offset", offset)
                .getResultList();
    }

    public long countAllNative() {
        return ((Number) s().createNativeQuery("select count(*) from product").getSingleResult()).longValue();
    }

    public List<Product> findFiltered(
            String name, String partNumber, String unitOfMeasureLike,
            String organizationName, String personName,
            int offset, int limit, String sort, String dir
    ) {
        java.util.List<ru.se.ifmo.is1.model.UnitOfMeasure> units = null;
        if (unitOfMeasureLike != null && !unitOfMeasureLike.isBlank()) {
            String n = unitOfMeasureLike.trim().toLowerCase();
            units = java.util.Arrays.stream(ru.se.ifmo.is1.model.UnitOfMeasure.values())
                    .filter(u -> u.name().toLowerCase().contains(n))
                    .toList();
            if (units.isEmpty()) return java.util.List.of();
        }

        StringBuilder hql = new StringBuilder("""
        select p
        from Product p
          left join p.manufacturer m
          left join p.owner o
        where 1=1
    """);

        if (name != null && !name.isBlank())              hql.append(" and lower(p.name)       like lower(:name) ");
        if (partNumber != null && !partNumber.isBlank())  hql.append(" and lower(p.partNumber) like lower(:partNumber) ");
        if (units != null)                                 hql.append(" and p.unitOfMeasure in (:units) ");
        if (organizationName != null && !organizationName.isBlank())
            hql.append(" and lower(m.name)       like lower(:orgName) ");
        if (personName != null && !personName.isBlank())
            hql.append(" and lower(o.name)       like lower(:personName) ");

        var built = ru.se.ifmo.is1.repository.util.SortSupport.build(SORT, sort, dir, "id");
        String orderBy = built.orderBy();
        hql.append(" order by ").append(orderBy);

        var q = s().createQuery(hql.toString(), Product.class);
        if (name != null && !name.isBlank())              q.setParameter("name", "%" + name.trim() + "%");
        if (partNumber != null && !partNumber.isBlank())  q.setParameter("partNumber", "%" + partNumber.trim() + "%");
        if (units != null)                                 q.setParameterList("units", units);
        if (organizationName != null && !organizationName.isBlank()) q.setParameter("orgName", "%" + organizationName.trim() + "%");
        if (personName != null && !personName.isBlank())             q.setParameter("personName", "%" + personName.trim() + "%");

        q.setFirstResult(Math.max(offset, 0));
        q.setMaxResults(Math.max(limit, 1));
        return q.list();
    }

    public long countFiltered(
            String name, String partNumber, String unitOfMeasureLike,
            String organizationName, String personName
    ) {
        java.util.List<ru.se.ifmo.is1.model.UnitOfMeasure> units = null;
        if (unitOfMeasureLike != null && !unitOfMeasureLike.isBlank()) {
            String n = unitOfMeasureLike.trim().toLowerCase();
            units = java.util.Arrays.stream(ru.se.ifmo.is1.model.UnitOfMeasure.values())
                    .filter(u -> u.name().toLowerCase().contains(n))
                    .toList();
            if (units.isEmpty()) return 0L;
        }

        StringBuilder hql = new StringBuilder("""
        select count(p.id)
        from Product p
          left join p.manufacturer m
          left join p.owner o
        where 1=1
    """);

        if (name != null && !name.isBlank())              hql.append(" and lower(p.name)       like lower(:name) ");
        if (partNumber != null && !partNumber.isBlank())  hql.append(" and lower(p.partNumber) like lower(:partNumber) ");
        if (units != null)                                 hql.append(" and p.unitOfMeasure in (:units) ");
        if (organizationName != null && !organizationName.isBlank())
            hql.append(" and lower(m.name)       like lower(:orgName) ");
        if (personName != null && !personName.isBlank())
            hql.append(" and lower(o.name)       like lower(:personName) ");

        var q = s().createQuery(hql.toString(), Long.class);
        if (name != null && !name.isBlank())              q.setParameter("name", "%" + name.trim() + "%");
        if (partNumber != null && !partNumber.isBlank())  q.setParameter("partNumber", "%" + partNumber.trim() + "%");
        if (units != null)                                 q.setParameterList("units", units);
        if (organizationName != null && !organizationName.isBlank()) q.setParameter("orgName", "%" + organizationName.trim() + "%");
        if (personName != null && !personName.isBlank())             q.setParameter("personName", "%" + personName.trim() + "%");

        return q.getSingleResult();
    }

}
