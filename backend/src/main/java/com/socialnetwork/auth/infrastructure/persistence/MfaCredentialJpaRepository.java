package com.socialnetwork.auth.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MfaCredentialJpaRepository extends JpaRepository<MfaCredentialEntity, UUID> {

    void deleteByUserId(UUID userId);
}
