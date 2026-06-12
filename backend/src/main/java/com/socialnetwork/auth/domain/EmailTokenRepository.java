package com.socialnetwork.auth.domain;

import java.util.Optional;

/** Puerto de persistencia de tokens de email. Adaptador JPA en {@code infrastructure.persistence}. */
public interface EmailTokenRepository {

    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);

    EmailToken save(EmailToken token);
}
