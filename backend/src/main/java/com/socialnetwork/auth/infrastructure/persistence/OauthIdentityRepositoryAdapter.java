package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.OauthIdentity;
import com.socialnetwork.auth.domain.OauthIdentityRepository;
import com.socialnetwork.auth.domain.OauthProvider;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link OauthIdentityRepository} sobre Spring Data JPA. */
@Component
class OauthIdentityRepositoryAdapter implements OauthIdentityRepository {

    private final OauthIdentityJpaRepository jpa;

    OauthIdentityRepositoryAdapter(OauthIdentityJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<OauthIdentity> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId) {
        return jpa.findByProviderAndProviderUserId(provider, providerUserId).map(OauthIdentityEntity::toDomain);
    }

    @Override
    public OauthIdentity save(OauthIdentity identity) {
        return jpa.save(OauthIdentityEntity.fromDomain(identity)).toDomain();
    }
}
