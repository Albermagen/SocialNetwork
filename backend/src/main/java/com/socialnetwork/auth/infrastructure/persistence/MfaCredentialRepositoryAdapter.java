package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.MfaCredential;
import com.socialnetwork.auth.domain.MfaCredentialRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link MfaCredentialRepository} sobre Spring Data JPA. */
@Component
class MfaCredentialRepositoryAdapter implements MfaCredentialRepository {

    private final MfaCredentialJpaRepository jpa;

    MfaCredentialRepositoryAdapter(MfaCredentialJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<MfaCredential> findByUserId(UUID userId) {
        return jpa.findById(userId).map(MfaCredentialEntity::toDomain);
    }

    @Override
    public MfaCredential save(MfaCredential credential) {
        return jpa.save(MfaCredentialEntity.fromDomain(credential)).toDomain();
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUserId(userId);
    }
}
