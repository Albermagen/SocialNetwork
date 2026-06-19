package com.socialnetwork.auth.domain;

import java.util.Optional;

/** Puerto de persistencia de identidades externas. Adaptador JPA en {@code infrastructure.persistence}. */
public interface OauthIdentityRepository {

    Optional<OauthIdentity> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId);

    OauthIdentity save(OauthIdentity identity);
}
