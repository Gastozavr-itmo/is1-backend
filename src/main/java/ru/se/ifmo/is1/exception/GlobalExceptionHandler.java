package ru.se.ifmo.is1.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var pd = baseProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, "Validation failed", req);
        var errors = new ArrayList<FieldError>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> errors.add(new FieldError(fe.getField(), fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors().forEach(ge -> errors.add(new FieldError(ge.getObjectName(), ge.getDefaultMessage())));
        addErrors(pd, errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        var pd = baseProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, "Validation failed", req);
        var errors = new ArrayList<FieldError>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.add(new FieldError(String.valueOf(v.getPropertyPath()), v.getMessage()));
        }
        addErrors(pd, errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.BAD_REQUEST, "Malformed JSON or invalid request body", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.METHOD_NOT_ALLOWED, ApiErrorCode.BAD_REQUEST, "HTTP method not allowed for this endpoint", req);
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.NOT_FOUND, ApiErrorCode.ENTITY_NOT_FOUND, safeMsg(ex, "Entity not found"), req);
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, safeMsg(ex, "Validation failed"), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, safeMsg(ex, "Validation failed"), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        var code = ApiErrorCode.DATA_INTEGRITY_VIOLATION;
        var message = "Database constraint violated";
        var root = rootCause(ex);
        var low = (root.getMessage() == null ? "" : root.getMessage()).toLowerCase();

        if (low.contains("unique") || low.contains("duplicate key") || low.contains("uq_")) {
            code = ApiErrorCode.UNIQUE_CONSTRAINT_VIOLATION;
            message = "Unique constraint violated";
        }
        return baseProblem(HttpStatus.CONFLICT, code, message, req);
    }

    @ExceptionHandler(UnexpectedRollbackException.class)
    public ProblemDetail handleUnexpectedRollback(UnexpectedRollbackException ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.CONFLICT, ApiErrorCode.DATA_INTEGRITY_VIOLATION, "Transaction rolled back due to data conflict", req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAny(Exception ex, HttpServletRequest req) {
        return baseProblem(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR, "Internal server error", req);
    }


    private ProblemDetail baseProblem(HttpStatus status, ApiErrorCode code, String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create("about:blank"));
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code.name());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }

    private void addErrors(ProblemDetail pd, List<FieldError> errors) {
        if (errors != null && !errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
    }

    private String safeMsg(Throwable ex, String fallback) {
        String m = ex.getMessage();
        return (m == null || m.isBlank()) ? fallback : m;
    }

    private Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c;
    }

    public static final class FieldError {
        public final String field;
        public final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
