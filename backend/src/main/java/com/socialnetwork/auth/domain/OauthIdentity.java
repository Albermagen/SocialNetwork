package com.socialnetwork.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Vínculo entre una cuenta local ({@link UserAccount}) y una identidad en un proveedor externo.
 * Identificada de forma única por {@code (provider, providerUserId)}. Dominio puro (sin framework).
 */
public class OauthIdentity {

    private final UUID id;
    private final UUID userId;
    private final OauthProvider provider;
    private final String providerUserId;
    private final String email;
    private final Instant createdAt;

    private OauthIdentity(
            UUID id, UUID userId, OauthProvider provider, String providerUserId, String email, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.createdAt = createdAt;
    }

    /** Crea un vínculo nuevo entre una cuenta y una identidad externa. */
    public static OauthIdentity link(
            UUID id, UUID userId, OauthProvider provider, String providerUserId, String email) {
        if (id == null || userId == null || provider == null || providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("id, userId, provider y providerUserId son obligatorios");
        }
        return new OauthIdentity(id, userId, provider, providerUserId, email, Instant.now());
    }

    /** Reconstrucción desde persistencia. */
    public static OauthIdentity restore(
            UUID id, UUID userId, OauthProvider provider, String providerUserId, String email, Instant createdAt) {
        return new OauthIdentity(id, userId, provider, providerUserId, email, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public OauthProvider provider() {
        return provider;
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String email() {
        return email;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
