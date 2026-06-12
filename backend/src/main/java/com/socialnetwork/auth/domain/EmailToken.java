package com.socialnetwork.auth.domain;

import com.socialnetwork.shared.error.BusinessRuleException;
import java.time.Instant;
import java.util.UUID;

/**
 * Token de un solo uso enviado por email (verificación de cuenta o reset de contraseña). Solo se
 * persiste su hash SHA-256: una fuga de la BD no permite usar los tokens.
 */
public class EmailToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final EmailTokenType type;
    private final Instant expiresAt;
    private Instant usedAt;
    private final Instant createdAt;

    private EmailToken(
            UUID id,
            UUID userId,
            String tokenHash,
            EmailTokenType type,
            Instant expiresAt,
            Instant usedAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static EmailToken issue(UUID id, UUID userId, String tokenHash, EmailTokenType type, Instant expiresAt) {
        return new EmailToken(id, userId, tokenHash, type, expiresAt, null, Instant.now());
    }

    public static EmailToken restore(
            UUID id,
            UUID userId,
            String tokenHash,
            EmailTokenType type,
            Instant expiresAt,
            Instant usedAt,
            Instant createdAt) {
        return new EmailToken(id, userId, tokenHash, type, expiresAt, usedAt, createdAt);
    }

    /** Consume el token. Falla si ya se usó o ha caducado (mensaje único: no dar pistas). */
    public void consume(Instant now) {
        if (usedAt != null || now.isAfter(expiresAt)) {
            throw new BusinessRuleException("invalid_token", "El token no es válido o ha caducado");
        }
        this.usedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public EmailTokenType type() {
        return type;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
