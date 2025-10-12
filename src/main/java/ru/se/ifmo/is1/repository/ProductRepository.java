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
    private Session s(){ return sf.getCurrentSession(); }

    public Optional<Product> findById(Long id){ return Optional.ofNullable(s().get(Product.class, id)); }
    public Long save(Product e){ s().persist(e); return e.getId(); }
    public Long merge(Product e){ return ((Product) s().merge(e)).getId(); }
    public void delete(Product e){ s().remove(e); }

    private static final Map<String, SortSupport.Rule> SORT = new LinkedHashMap<>();
    static {
        // product.*
        SORT.put("id",             SortSupport.Rule.column("p.id"));
        SORT.put("name",           SortSupport.Rule.column("p.name"));
        SORT.put("price",          SortSupport.Rule.column("p.price"));
        SORT.put("unitOfMeasure",  SortSupport.Rule.column("p.unit_of_measure"));
        SORT.put("creationDate",   SortSupport.Rule.column("p.creation_date"));
        SORT.put("createdAt",      SortSupport.Rule.column("p.created_at"));
        SORT.put("updatedAt",      SortSupport.Rule.column("p.updated_at"));

        SORT.put("organizationName", SortSupport.Rule.joined(
                "m.name",
                " left join organization m on m.id = p.manufacturer_id "
        ));

        SORT.put("personName", SortSupport.Rule.joined(
                "per.name",
                " left join person per on per.id = p.owner_id "
        ));
    }


    public List<Product> findPageNative(int offset, int size, String sort, String dir){
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

    public long countAllNative(){
        return ((Number) s().createNativeQuery("select count(*) from product").getSingleResult()).longValue();
    }
}
