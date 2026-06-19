package com.socialnetwork.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MfaRecoveryCodeJpaRepository extends JpaRepository<MfaRecoveryCodeEntity, UUID> {

    Optional<MfaRecoveryCodeEntity> findByUserIdAndCodeHashAndUsedAtIsNull(UUID userId, String codeHash);

    void deleteByUserId(UUID userId);
}
