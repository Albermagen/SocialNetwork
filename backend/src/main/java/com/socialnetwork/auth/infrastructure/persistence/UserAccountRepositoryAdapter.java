package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.UserAccount;
import com.socialnetwork.auth.domain.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link UserAccountRepository} sobre Spring Data JPA. */
@Component
class UserAccountRepositoryAdapter implements UserAccountRepository {

    private final UserAccountJpaRepository jpa;

    UserAccountRepositoryAdapter(UserAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return jpa.findById(id).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findByIdentifier(String identifier) {
        return jpa.findByIdentifier(identifier).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserAccountEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public UserAccount save(UserAccount account) {
        return jpa.save(UserAccountEntity.fromDomain(account)).toDomain();
    }
}
