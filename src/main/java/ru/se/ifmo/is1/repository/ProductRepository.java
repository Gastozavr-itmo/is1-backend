package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepository {
    private final SessionFactory sf;
    private Session s() { return sf.getCurrentSession(); }

    public Optional<Product> findById(Long id) { return Optional.ofNullable(s().get(Product.class, id)); }
    public List<Product> findAll() { return s().createQuery("from Product", Product.class).list(); }
    public Long save(Product p) { s().persist(p); return p.getId(); }
    public Long merge(Product p) { return ((Product) s().merge(p)).getId(); }
    public void delete(Product p) { s().remove(p); }
}
