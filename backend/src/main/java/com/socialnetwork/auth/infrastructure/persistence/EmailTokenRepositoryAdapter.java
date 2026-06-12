package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.EmailToken;
import com.socialnetwork.auth.domain.EmailTokenRepository;
import com.socialnetwork.auth.domain.EmailTokenType;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link EmailTokenRepository} sobre Spring Data JPA. */
@Component
class EmailTokenRepositoryAdapter implements EmailTokenRepository {

    private final EmailTokenJpaRepository jpa;

    EmailTokenRepositoryAdapter(EmailTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type) {
        return jpa.findByTokenHashAndType(tokenHash, type).map(EmailTokenEntity::toDomain);
    }

    @Override
    public EmailToken save(EmailToken token) {
        return jpa.save(EmailTokenEntity.fromDomain(token)).toDomain();
    }
}
