package ru.se.ifmo.is1.exception;

import jakarta.persistence.PersistenceException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.se.ifmo.is1.dto.imports.ImportResponse;
import ru.se.ifmo.is1.dto.imports.ValidationError;

import java.util.List;

@RestControllerAdvice
public class ImportExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ImportResponse> badRequest(IllegalArgumentException ex) {
        var err = new ValidationError(-1, "items", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ImportResponse.failed(0, List.of(err)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ImportResponse> internal(Exception ex) {
        var err = new ValidationError(-1, "items",
                "Import failed: " + rootCause(ex).getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ImportResponse.failed(0, List.of(err)));
    }

    private Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c;
    }
}
