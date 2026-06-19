package com.socialnetwork.auth.domain;

import com.socialnetwork.shared.error.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

/**
 * Credencial de segundo factor (TOTP) de un usuario. Nace en estado de enrolamiento ({@code
 * enabled=false}) y se activa al confirmar el primer código. Dominio puro (sin framework).
 */
public class MfaCredential {

    private final UUID userId;
    private final String secret;
    private boolean enabled;
    private final Instant createdAt;
    private Instant confirmedAt;

    private MfaCredential(UUID userId, String secret, boolean enabled, Instant createdAt, Instant confirmedAt) {
        this.userId = userId;
        this.secret = secret;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
    }

    /** Inicia el enrolamiento con un secreto recién generado (aún sin activar). */
    public static MfaCredential startEnrollment(UUID userId, String secret) {
        if (userId == null || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("userId y secret son obligatorios");
        }
        return new MfaCredential(userId, secret, false, Instant.now(), null);
    }

    public static MfaCredential restore(
            UUID userId, String secret, boolean enabled, Instant createdAt, Instant confirmedAt) {
        return new MfaCredential(userId, secret, enabled, createdAt, confirmedAt);
    }

    /** Activa el segundo factor tras verificar el primer código. Idempotente. */
    public void confirm() {
        if (!enabled) {
            this.enabled = true;
            this.confirmedAt = Instant.now();
        }
    }

    /** Garantiza que la credencial esté activa antes de operaciones que lo requieran. */
    public void requireEnabled() {
        if (!enabled) {
            throw new BusinessRuleException("mfa_not_enabled", "El segundo factor no está activo");
        }
    }

    public UUID userId() {
        return userId;
    }

    public String secret() {
        return secret;
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant confirmedAt() {
        return confirmedAt;
    }
}
