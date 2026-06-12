package com.socialnetwork.shared.error;

/** Fallo de autenticación (credenciales o tokens inválidos). Se mapea a HTTP 401. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
