package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.EmailTokenType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EmailTokenJpaRepository extends JpaRepository<EmailTokenEntity, UUID> {

    Optional<EmailTokenEntity> findByTokenHashAndType(String tokenHash, EmailTokenType type);
}
