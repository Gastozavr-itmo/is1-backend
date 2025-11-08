package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.model.ImportOperation;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ImportOperationRepository {

    private final SessionFactory sessionFactory;
    private Session s() { return sessionFactory.getCurrentSession(); }

    @Transactional
    public void save(ImportOperation op) {
        s().save(op);
    }

    @Transactional(readOnly = true)
    public ImportOperation findById(Long id) {
        return s().get(ImportOperation.class, id);
    }

    @Transactional(readOnly = true)
    public List<ImportOperation> findAll() {
        return s().createQuery(
                        "select io from ImportOperation io order by io.id desc", ImportOperation.class)
                .list();
    }
}
