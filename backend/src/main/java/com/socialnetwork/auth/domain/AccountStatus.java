package com.socialnetwork.auth.domain;

/** Ciclo de vida de la cuenta. {@code DELETED} es soft delete: conserva la fila por integridad. */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
