package com.socialnetwork.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {

    /** Las columnas son citext: la comparación ya es case-insensitive en Postgres. */
    @Query("select u from UserAccountEntity u where u.username = :identifier or u.email = :identifier")
    Optional<UserAccountEntity> findByIdentifier(@Param("identifier") String identifier);

    Optional<UserAccountEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
