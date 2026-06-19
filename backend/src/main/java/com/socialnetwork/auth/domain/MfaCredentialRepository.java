package com.socialnetwork.auth.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de credenciales MFA. Adaptador JPA en {@code infrastructure.persistence}. */
public interface MfaCredentialRepository {

    Optional<MfaCredential> findByUserId(UUID userId);

    MfaCredential save(MfaCredential credential);

    void deleteByUserId(UUID userId);
}
