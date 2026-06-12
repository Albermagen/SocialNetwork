package com.socialnetwork.shared.error;

/** Identidad válida pero sin permiso para la operación. Se mapea a HTTP 403. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String code, String message) {
        super(code, message);
    }
}
