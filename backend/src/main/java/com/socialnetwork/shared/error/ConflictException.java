package com.socialnetwork.shared.error;

/** Conflicto con el estado actual (duplicados, versiones obsoletas). Se mapea a HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
