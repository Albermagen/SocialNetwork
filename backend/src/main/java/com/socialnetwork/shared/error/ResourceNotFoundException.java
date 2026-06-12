package com.socialnetwork.shared.error;

/** El recurso solicitado no existe. Se mapea a HTTP 404. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
