package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.OauthIdentity;
import com.socialnetwork.auth.domain.OauthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de {@link OauthIdentity}. */
@Entity
@Table(name = "oauth_identities")
class OauthIdentityEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OauthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(columnDefinition = "citext")
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OauthIdentityEntity() {}

    static OauthIdentityEntity fromDomain(OauthIdentity identity) {
        OauthIdentityEntity entity = new OauthIdentityEntity();
        entity.id = identity.id();
        entity.userId = identity.userId();
        entity.provider = identity.provider();
        entity.providerUserId = identity.providerUserId();
        entity.email = identity.email();
        entity.createdAt = identity.createdAt();
        return entity;
    }

    OauthIdentity toDomain() {
        return OauthIdentity.restore(id, userId, provider, providerUserId, email, createdAt);
    }
}
