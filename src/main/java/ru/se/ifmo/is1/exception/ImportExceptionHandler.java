package ru.se.ifmo.is1.exception;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.PersistenceException;
import org.springframework.transaction.UnexpectedRollbackException;
import ru.se.ifmo.is1.dto.imports.ImportResponse;
import ru.se.ifmo.is1.dto.imports.ValidationError;

import java.util.List;

@ControllerAdvice
public class ImportExceptionHandler {

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            PersistenceException.class,
            IllegalArgumentException.class,
            UnexpectedRollbackException.class,
            Exception.class
    })
    public ResponseEntity<ImportResponse> handleImportErrors(Exception ex) {
        var msg = rootCause(ex).getMessage();
        var err = new ValidationError(-1, "items", "Import failed: " + msg);
        return ResponseEntity.ok(ImportResponse.failed(0, List.of(err)));
    }

    private Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c;
    }
}