package com.socialnetwork.shared.web;

import com.socialnetwork.shared.error.BusinessRuleException;
import com.socialnetwork.shared.error.ConflictException;
import com.socialnetwork.shared.error.DomainException;
import com.socialnetwork.shared.error.ResourceNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Manejo global de errores con RFC 9457 (Problem Details). Las excepciones estándar de Spring MVC
 * las resuelve {@link ResponseEntityExceptionHandler} (activado con
 * {@code spring.mvc.problemdetails.enabled=true}); aquí se añaden las excepciones de dominio y el
 * fallback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_TYPE_BASE = "https://socialnetwork.dev/problems/";

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problemOf(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        return problemOf(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problemOf(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        return problemOf(HttpStatus.BAD_REQUEST, ex);
    }

    /** Fallback: nunca filtrar detalles internos al cliente (OWASP). */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno inesperado");
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "internal_error"));
        problem.setProperty("code", "internal_error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /** Enriquecer errores de validación con el detalle por campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "validation_error"));
        problem.setProperty("code", "validation_error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty(
                "errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> Map.of(
                                "field", error.getField(),
                                "message", String.valueOf(error.getDefaultMessage())))
                        .toList());
        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    private ProblemDetail problemOf(HttpStatus status, DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create(PROBLEM_TYPE_BASE + ex.code()));
        problem.setProperty("code", ex.code());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
