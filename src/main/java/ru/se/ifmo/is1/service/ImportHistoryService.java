package ru.se.ifmo.is1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.se.ifmo.is1.dto.imports.ImportOperationDTO;
import ru.se.ifmo.is1.dto.paging.PageResponseDTO;
import ru.se.ifmo.is1.model.ImportOperation;
import ru.se.ifmo.is1.model.ImportStatus;
import ru.se.ifmo.is1.repository.ImportOperationRepository;
import ru.se.ifmo.is1.ws.ChangePublisher;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportHistoryService {
    private final ImportOperationRepository repo;
    private final ChangePublisher changes;

    @Transactional(readOnly = true)
    public List<ImportOperation> list() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ImportOperationDTO> list(int page, int size, String sort, String dir) {
        int p = Math.max(page, 0);
        int s = Math.max(size, 1);
        long total = repo.countAll();

        var items = repo.findPage(p, s, sort, dir)
                .stream()
                .map(ImportOperationDTO::from)
                .toList();

        return PageResponseDTO.of(items, p, s, total, sort, dir);
    }

    @Transactional(readOnly = true)
    public ImportOperation find(Long id) {
        ImportOperation op = repo.findById(id);
        if (op == null) throw new IllegalArgumentException("Import operation not found: " + id);
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
        afterCommit(() -> changes.broadcast("imports", "updated", op.getId()));
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
        afterCommit(() -> changes.broadcast("imports", "updated", op.getId()));
    }

    private void afterCommit(Runnable r) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                r.run();
            }
        });
    }
}
