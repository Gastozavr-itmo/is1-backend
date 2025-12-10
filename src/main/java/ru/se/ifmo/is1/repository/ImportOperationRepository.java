package ru.se.ifmo.is1.repository;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import ru.se.ifmo.is1.model.ImportOperation;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ImportOperationRepository {

    private final SessionFactory sessionFactory;

    private Session s() {
        return sessionFactory.getCurrentSession();
    }

    public void save(ImportOperation op) {
        s().persist(op);
    }

    public ImportOperation findById(Long id) {
        return s().get(ImportOperation.class, id);
    }

    public long countAll() {
        Long cnt = s().createQuery("select count(io.id) from ImportOperation io", Long.class)
                .uniqueResult();
        return cnt == null ? 0L : cnt;
    }

    public List<ImportOperation> findPage(int page, int size, String sort, String dir) {
        int p = Math.max(page, 0);
        int s = Math.max(size, 1);

        String orderBy = buildOrderBy(sort, dir);
        String hql = "select io from ImportOperation io " + orderBy;

        return this.s().createQuery(hql, ImportOperation.class)
                .setFirstResult(p * s)
                .setMaxResults(s)
                .list();
    }

    private String buildOrderBy(String sort, String dir) {
        Map<String, String> fields = Map.of(
                "id", "io.id",
                "startedAt", "io.startedAt",
                "finishedAt", "io.finishedAt",
                "status", "io.status",
                "createdCount", "io.createdCount"
        );
        String f = fields.getOrDefault(sort, "io.id");
        String d = ("asc".equalsIgnoreCase(dir)) ? "asc" : "desc";
        return "order by " + f + " " + d;
    }
}
