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

    // ==== ВСЕ КОЛОНКИ ORGANIZATION + имя Person (только name) ====
    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();
    static {
        SORT.put("id",               SortSupport.Rule.column("o.id"));
        SORT.put("name",             SortSupport.Rule.column("o.name"));
        SORT.put("annualTurnover",   SortSupport.Rule.column("o.annual_turnover"));
        SORT.put("employeesCount",   SortSupport.Rule.column("o.employees_count"));
        SORT.put("type",             SortSupport.Rule.column("o.type"));
        SORT.put("createdAt",        SortSupport.Rule.column("o.created_at"));
        SORT.put("updatedAt",        SortSupport.Rule.column("o.updated_at"));

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
}
