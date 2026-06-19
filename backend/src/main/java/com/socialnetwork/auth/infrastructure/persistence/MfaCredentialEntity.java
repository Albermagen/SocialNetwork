package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.MfaCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de {@link MfaCredential} (una fila por usuario). */
@Entity
@Table(name = "mfa_credentials")
class MfaCredentialEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected MfaCredentialEntity() {}

    static MfaCredentialEntity fromDomain(MfaCredential credential) {
        MfaCredentialEntity entity = new MfaCredentialEntity();
        entity.userId = credential.userId();
        entity.secret = credential.secret();
        entity.enabled = credential.enabled();
        entity.createdAt = credential.createdAt();
        entity.confirmedAt = credential.confirmedAt();
        return entity;
    }

    MfaCredential toDomain() {
        return MfaCredential.restore(userId, secret, enabled, createdAt, confirmedAt);
    }
}
