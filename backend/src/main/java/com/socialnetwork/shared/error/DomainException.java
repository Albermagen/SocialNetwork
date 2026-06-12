package com.socialnetwork.shared.error;

/**
 * Base de las excepciones de negocio. Cada módulo define las suyas extendiendo las subclases
 * semánticas ({@link ResourceNotFoundException}, {@link ConflictException},
 * {@link BusinessRuleException}); el mapeo a HTTP/Problem Details es transversal y vive en
 * {@code shared.web.GlobalExceptionHandler}.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Código estable y legible por máquina (p. ej. {@code user_not_found}). */
    public String code() {
        return code;
    }
}
