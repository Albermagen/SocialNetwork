package com.socialnetwork.auth.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de tokens de email. Adaptador JPA en {@code infrastructure.persistence}. */
public interface EmailTokenRepository {

    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);

    EmailToken save(EmailToken token);

    /** Invalida los tokens pendientes de un tipo para un usuario (un nuevo reset anula los previos). */
    void deleteByUserIdAndType(UUID userId, EmailTokenType type);
}
