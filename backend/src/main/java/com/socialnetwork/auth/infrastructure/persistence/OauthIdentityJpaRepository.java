package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.OauthProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OauthIdentityJpaRepository extends JpaRepository<OauthIdentityEntity, UUID> {

    Optional<OauthIdentityEntity> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId);
}
