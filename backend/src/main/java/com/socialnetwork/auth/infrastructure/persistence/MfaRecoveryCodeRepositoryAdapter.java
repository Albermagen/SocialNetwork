package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.MfaRecoveryCodeRepository;
import com.socialnetwork.shared.util.Uuids;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador del puerto {@link MfaRecoveryCodeRepository} sobre Spring Data JPA. */
@Component
class MfaRecoveryCodeRepositoryAdapter implements MfaRecoveryCodeRepository {

    private final MfaRecoveryCodeJpaRepository jpa;

    MfaRecoveryCodeRepositoryAdapter(MfaRecoveryCodeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void replaceAll(UUID userId, List<String> codeHashes) {
        jpa.deleteByUserId(userId);
        List<MfaRecoveryCodeEntity> entities = codeHashes.stream()
                .map(hash -> MfaRecoveryCodeEntity.create(Uuids.v7(), userId, hash))
                .toList();
        jpa.saveAll(entities);
    }

    @Override
    @Transactional
    public boolean consume(UUID userId, String codeHash) {
        return jpa.findByUserIdAndCodeHashAndUsedAtIsNull(userId, codeHash)
                .map(entity -> {
                    entity.markUsed(Instant.now());
                    jpa.save(entity);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUserId(userId);
    }
}
