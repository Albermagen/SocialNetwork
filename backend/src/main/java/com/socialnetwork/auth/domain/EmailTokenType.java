package com.socialnetwork.auth.domain;

/** Propósito de un token enviado por email. {@code RESET} se usará en la iteración de recuperación. */
public enum EmailTokenType {
    VERIFY,
    RESET
}
