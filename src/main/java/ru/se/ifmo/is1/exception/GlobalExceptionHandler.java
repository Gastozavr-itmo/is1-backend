package ru.se.ifmo.is1.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.se.ifmo.is1.dto.error.ExceptionResponse;
import ru.se.ifmo.is1.dto.imports.ValidationError;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 400 — бизнес-валидация (твоя логика) =====
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ExceptionResponse> handleBadRequest(RuntimeException ex,
                                                              HttpServletRequest request) {
        String msg = ex.getMessage();
        var details = List.of(new ValidationError(-1, "items", msg));
        return build(HttpStatus.BAD_REQUEST, msg, request, details);
    }

    // ===== 400 — @Valid на DTO (RequestBody / ModelAttribute) =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        List<ValidationError> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.add(new ValidationError(-1, fe.getField(), fe.getDefaultMessage()))
        );
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ExceptionResponse> handleBindException(BindException ex,
                                                                 HttpServletRequest request) {
        List<ValidationError> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                details.add(new ValidationError(-1, fe.getField(), fe.getDefaultMessage()))
        );
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    // ===== 400 — Bean Validation на сущностях (внутри транзакции) =====

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                       HttpServletRequest request) {
        return buildFromConstraintViolation(ex, request);
    }

    // ===== 404 — сущность не найдена =====

    @ExceptionHandler({EntityNotFoundException.class, java.util.NoSuchElementException.class})
    public ResponseEntity<ExceptionResponse> handleNotFound(RuntimeException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Object not found", request, List.of());
    }

    // ===== 409 — транзакция откатилась из-за целостности БД =====
    //
    // Ловим:
    //  - TransactionSystemException / UnexpectedRollbackException (как в WildFly)
    //  - прямой DataIntegrityViolationException, если Spring его кинул сразу
    //
    @ExceptionHandler({
            TransactionSystemException.class,
            UnexpectedRollbackException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ExceptionResponse> handleTxRollback(RuntimeException ex,
                                                              HttpServletRequest request) {

        Throwable root = rootCause(ex);

        // Если это Bean Validation — отдаём как 400 "Validation failed"
        if (root instanceof ConstraintViolationException cve) {
            return buildFromConstraintViolation(cve, request);
        }

        // Пробуем достать SQL-исключение
        SQLException sqlEx = extractSQLException(root);

        String message = "Транзакция отменена. Изменения не были сохранены.";
        HttpStatus status = HttpStatus.CONFLICT;

        if (sqlEx != null) {
            String sqlState = sqlEx.getSQLState();
            String sqlMsg   = sqlEx.getMessage();

            // 23503 — FK violation: попытка удалить/обновить объект, на который есть ссылки
            if ("23503".equals(sqlState)) {
                if (sqlMsg != null && sqlMsg.contains("product_owner_id_fkey")) {
                    message = "Невозможно удалить владельца: на него ссылаются продукты. Транзакция отменена.";
                } else if (sqlMsg != null && sqlMsg.contains("product_manufacturer_id_fkey")) {
                    message = "Невозможно удалить производителя: на него ссылаются продукты. Транзакция отменена.";
                } else {
                    message = "Невозможно удалить или изменить объект: он используется в других записях. Транзакция отменена.";
                }
            }
            // 23505 — UNIQUE violation
            else if ("23505".equals(sqlState)) {
                message = "Объект с таким уникальным значением уже существует. Транзакция отменена.";
            }
            // 23502 — NOT NULL violation
            else if ("23502".equals(sqlState)) {
                message = "Не заполнено обязательное поле. Транзакция отменена.";
            }
        }

        var details = List.of(new ValidationError(-1, "items", message));
        return build(status, message, request, details);
    }

    // ===== 500 — прочие ошибки БД (соединение и т.п.) =====

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ExceptionResponse> handleDataAccess(DataAccessException ex,
                                                              HttpServletRequest request) {
        String message = "Ошибка при обращении к базе данных. Транзакция могла быть отменена.";
        var details = List.of(new ValidationError(-1, "items", message));
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message, request, details);
    }

    // ===== 500 — общий fallback =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleInternal(Exception ex,
                                                            HttpServletRequest request) {
        ex.printStackTrace(); // позже заменишь на логгер

        String message = "Internal server error. Transaction may have been rolled back.";
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message, request, List.of());
    }

    // ===== helpers =====

    private ResponseEntity<ExceptionResponse> buildFromConstraintViolation(ConstraintViolationException ex,
                                                                           HttpServletRequest request) {
        List<ValidationError> details = new ArrayList<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            String path = cv.getPropertyPath() != null
                    ? cv.getPropertyPath().toString()
                    : "items";
            details.add(new ValidationError(-1, path, cv.getMessage()));
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    private ResponseEntity<ExceptionResponse> build(HttpStatus status,
                                                    String message,
                                                    HttpServletRequest request,
                                                    List<ValidationError> details) {
        ExceptionResponse body = ExceptionResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .details(details == null ? List.of() : details)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c;
    }

    private SQLException extractSQLException(Throwable t) {
        Throwable cur = t;
        while (cur != null && cur != cur.getCause()) {
            if (cur instanceof SQLException sql) {
                return sql;
            }
            cur = cur.getCause();
        }
        return null;
    }
}
