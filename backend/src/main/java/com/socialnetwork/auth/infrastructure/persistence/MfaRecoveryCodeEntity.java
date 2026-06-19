package com.socialnetwork.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de un código de recuperación MFA (solo se guarda el hash). */
@Entity
@Table(name = "mfa_recovery_codes")
class MfaRecoveryCodeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MfaRecoveryCodeEntity() {}

    static MfaRecoveryCodeEntity create(UUID id, UUID userId, String codeHash) {
        MfaRecoveryCodeEntity entity = new MfaRecoveryCodeEntity();
        entity.id = id;
        entity.userId = userId;
        entity.codeHash = codeHash;
        entity.usedAt = null;
        entity.createdAt = Instant.now();
        return entity;
    }

    void markUsed(Instant when) {
        this.usedAt = when;
    }

    UUID id() {
        return id;
    }
}
