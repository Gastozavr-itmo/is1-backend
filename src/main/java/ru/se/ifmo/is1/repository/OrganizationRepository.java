package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Organization;
import ru.se.ifmo.is1.repository.util.SortSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizationRepository {
    private final SessionFactory sf;
    private Session s(){ return sf.getCurrentSession(); }

    public Optional<Organization> findById(Integer id){ return Optional.ofNullable(s().get(Organization.class, id)); }
    public Integer save(Organization e){ s().persist(e); return e.getId(); }
    public Integer merge(Organization e){ return ((Organization) s().merge(e)).getId(); }
    public void delete(Organization e){ s().remove(e); }

    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();
    static {
        SORT.put("id",             SortSupport.Rule.column("o.id"));
        SORT.put("name",           SortSupport.Rule.column("o.name"));
        SORT.put("fullName",       SortSupport.Rule.column("o.fullName"));
        SORT.put("employeesCount", SortSupport.Rule.column("o.employeesCount"));
        SORT.put("annualTurnover", SortSupport.Rule.column("o.annualTurnover"));
        SORT.put("rating",         SortSupport.Rule.column("o.rating"));
        SORT.put("createdAt",   SortSupport.Rule.column("o.createdAt"));
        SORT.put("officialCity",   SortSupport.Rule.column("o.officialAddress.town.name"));
        SORT.put("postalCity",     SortSupport.Rule.column("o.postalAddress.town.name"));

    }

    public List<Organization> findPageNative(int offset, int size, String sort, String dir){
        var built = SortSupport.build(SORT, sort, dir, "id");
        String joins = String.join(" ", built.joins());
        String sql = "select o.* from organization o " + joins +
                " order by " + built.orderBy() +
                " limit :size offset :offset";
        return s().createNativeQuery(sql, Organization.class)
                .setParameter("size", size)
                .setParameter("offset", offset)
                .getResultList();
    }

    public long countAllNative(){
        return ((Number) s().createNativeQuery("select count(*) from organization").getSingleResult()).longValue();
    }
    public List<ru.se.ifmo.is1.model.Organization> findFiltered(
            String name,
            String fullName,
            String officialTownName,
            String postalTownName,
            int offset,
            int limit,
            String sort,
            String dir
    ) {
        StringBuilder hql = new StringBuilder("""
        select o
        from Organization o
          left join o.officialAddress oa
          left join oa.town oat
          left join o.postalAddress pa
          left join pa.town pat
        where 1=1
    """);

        if (name != null && !name.isBlank())        hql.append(" and lower(o.name)      like lower(:name) ");
        if (fullName != null && !fullName.isBlank())hql.append(" and lower(o.fullName)  like lower(:fullName) ");
        if (officialTownName != null && !officialTownName.isBlank())
            hql.append(" and lower(oat.name)     like lower(:officialTownName) ");
        if (postalTownName != null && !postalTownName.isBlank())
            hql.append(" and lower(pat.name)     like lower(:postalTownName) ");

        var built   = ru.se.ifmo.is1.repository.util.SortSupport.build(SORT, sort, dir, "id");
        hql.append(" order by ").append(built.orderBy());

        var q = s().createQuery(hql.toString(), ru.se.ifmo.is1.model.Organization.class);

        if (name != null && !name.isBlank())         q.setParameter("name", "%" + name.trim() + "%");
        if (fullName != null && !fullName.isBlank()) q.setParameter("fullName", "%" + fullName.trim() + "%");
        if (officialTownName != null && !officialTownName.isBlank())
            q.setParameter("officialTownName", "%" + officialTownName.trim() + "%");
        if (postalTownName != null && !postalTownName.isBlank())
            q.setParameter("postalTownName", "%" + postalTownName.trim() + "%");

        q.setFirstResult(Math.max(offset, 0));
        q.setMaxResults(Math.max(limit, 1));
        return q.list();
    }

    public long countFiltered(
            String name,
            String fullName,
            String officialTownName,
            String postalTownName
    ) {
        StringBuilder hql = new StringBuilder("""
        select count(o.id)
        from Organization o
          left join o.officialAddress oa
          left join oa.town oat
          left join o.postalAddress pa
          left join pa.town pat
        where 1=1
    """);

        if (name != null && !name.isBlank())        hql.append(" and lower(o.name)      like lower(:name) ");
        if (fullName != null && !fullName.isBlank())hql.append(" and lower(o.fullName)  like lower(:fullName) ");
        if (officialTownName != null && !officialTownName.isBlank())
            hql.append(" and lower(oat.name)     like lower(:officialTownName) ");
        if (postalTownName != null && !postalTownName.isBlank())
            hql.append(" and lower(pat.name)     like lower(:postalTownName) ");

        var q = s().createQuery(hql.toString(), Long.class);

        if (name != null && !name.isBlank())         q.setParameter("name", "%" + name.trim() + "%");
        if (fullName != null && !fullName.isBlank()) q.setParameter("fullName", "%" + fullName.trim() + "%");
        if (officialTownName != null && !officialTownName.isBlank())
            q.setParameter("officialTownName", "%" + officialTownName.trim() + "%");
        if (postalTownName != null && !postalTownName.isBlank())
            q.setParameter("postalTownName", "%" + postalTownName.trim() + "%");

        return q.getSingleResult();
    }

}
