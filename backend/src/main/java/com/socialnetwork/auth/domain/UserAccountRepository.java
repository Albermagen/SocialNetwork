package com.socialnetwork.auth.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia del agregado. Adaptador JPA en {@code infrastructure.persistence}. */
public interface UserAccountRepository {

    Optional<UserAccount> findById(UUID id);

    /** Busca por username o email indistintamente (case-insensitive, columnas citext). */
    Optional<UserAccount> findByIdentifier(String identifier);

    Optional<UserAccount> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UserAccount save(UserAccount account);
}
