package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.EmailToken;
import com.socialnetwork.auth.domain.EmailTokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de {@link EmailToken}. */
@Entity
@Table(name = "email_tokens")
class EmailTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EmailTokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailTokenEntity() {}

    static EmailTokenEntity fromDomain(EmailToken token) {
        EmailTokenEntity entity = new EmailTokenEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.tokenHash = token.tokenHash();
        entity.type = token.type();
        entity.expiresAt = token.expiresAt();
        entity.usedAt = token.usedAt();
        entity.createdAt = token.createdAt();
        return entity;
    }

    EmailToken toDomain() {
        return EmailToken.restore(id, userId, tokenHash, type, expiresAt, usedAt, createdAt);
    }
}
