package ru.se.ifmo.is1.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Product;

import java.util.List;

@Repository
public class ProductOpsRepository {

    @PersistenceContext
    private EntityManager em;

    public Long deleteOneByRating(Number rating) {
        Object id = em.createNativeQuery("select fn_delete_one_product_by_rating(:r)")
                .setParameter("r", rating)
                .getSingleResult();
        return (id == null) ? null : Long.valueOf(String.valueOf(id));
    }

    public long countGroupByManufactureCost() {
        Object c = em.createNativeQuery(
                        "select count(*) from (select * from fn_group_by_manufacture_cost()) t")
                .getSingleResult();
        return ((Number) c).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> pageGroupByManufactureCost(String sort, boolean asc, int limit, int offset) {
        String sql = "select * from fn_group_by_manufacture_cost() t " +
                orderClauseForGroup(sort, asc) + " limit :limit offset :offset";
        return em.createNativeQuery(sql)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
    }

    private String orderClauseForGroup(String sort, boolean asc) {
        String col = switch (sort == null ? "" : sort.toLowerCase()) {
            case "count" -> "t.count";
            case "manufacture_cost", "manufacturecost" -> "t.manufacture_cost";
            default -> "t.manufacture_cost";
        };
        return " order by " + col + (asc ? " asc" : " desc");
    }

    public long countPartNumberGt(String pn) {
        Object c = em.createNativeQuery(
                        "select count(*) from fn_products_with_partnumber_gt(:pn)")
                .setParameter("pn", pn)
                .getSingleResult();
        return ((Number) c).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Product> pagePartNumberGt(String pn, String sort, boolean asc, int limit, int offset) {
        String sql = "select * from fn_products_with_partnumber_gt(:pn) p " +
                orderClauseForProduct(sort, asc) + " limit :limit offset :offset";
        return em.createNativeQuery(sql, Product.class)
                .setParameter("pn", pn)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
    }

    public long countByManufacturer(Integer orgId) {
        Object c = em.createNativeQuery(
                        "select count(*) from fn_products_by_manufacturer(:id)")
                .setParameter("id", orgId)
                .getSingleResult();
        return ((Number) c).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Product> pageByManufacturer(Integer orgId, String sort, boolean asc, int limit, int offset) {
        String sql = "select * from fn_products_by_manufacturer(:id) p " +
                orderClauseForProduct(sort, asc) + " limit :limit offset :offset";
        return em.createNativeQuery(sql, Product.class)
                .setParameter("id", orgId)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
    }

    public long countByPriceRange(Number min, Number max) {
        Object c = em.createNativeQuery(
                        "select count(*) from fn_products_by_price_range(:min,:max)")
                .setParameter("min", min)
                .setParameter("max", max)
                .getSingleResult();
        return ((Number) c).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Product> pageByPriceRange(Number min, Number max, String sort, boolean asc, int limit, int offset) {
        String sql = "select * from fn_products_by_price_range(:min,:max) p " +
                orderClauseForProduct(sort, asc) + " limit :limit offset :offset";
        return em.createNativeQuery(sql, Product.class)
                .setParameter("min", min)
                .setParameter("max", max)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
    }

    private String orderClauseForProduct(String sort, boolean asc) {
        String col = switch (sort == null ? "" : sort.toLowerCase()) {
            case "id" -> "p.id";
            case "name" -> "p.name";
            case "partnumber", "part_number" -> "p.part_number";
            case "price" -> "p.price";
            case "rating" -> "p.rating";
            case "unitofmeasure", "unit_of_measure" -> "p.unit_of_measure";
            case "creationdate", "creation_date" -> "p.creation_date";
            default -> "p.id";
        };
        return " order by " + col + (asc ? " asc" : " desc");
    }
}
