package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.se.ifmo.is1.model.ImportOperation;
import ru.se.ifmo.is1.model.ImportStatus;
import ru.se.ifmo.is1.repository.ImportOperationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportHistoryService {

    private final ImportOperationRepository repo;

    @Transactional(readOnly = true)
    public List<ImportOperation> list() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public ImportOperation find(Long id) {
        var op = repo.findById(id);
        if (op == null) throw new IllegalArgumentException("Import operation not found: id=" + id);
        return op;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(LocalDateTime startedAt, int createdCount) {
        var op = ImportOperation.builder()
                .status(ImportStatus.SUCCESS)
                .createdCount(createdCount)
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .build();
        repo.save(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(LocalDateTime startedAt) {
        var op = ImportOperation.builder()
                .status(ImportStatus.FAILED)
                .createdCount(null)
                .startedAt(startedAt)
                .finishedAt(LocalDateTime.now())
                .build();
        repo.save(op);
    }
}
