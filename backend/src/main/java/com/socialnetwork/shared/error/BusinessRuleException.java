package com.socialnetwork.shared.error;

/** Violación de una regla de negocio con entrada sintácticamente válida. Se mapea a HTTP 422. */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
