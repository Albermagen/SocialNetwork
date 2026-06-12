package com.socialnetwork.auth.infrastructure.persistence;

import com.socialnetwork.auth.domain.AccountStatus;
import com.socialnetwork.auth.domain.Role;
import com.socialnetwork.auth.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de {@link UserAccount}. El dominio queda libre de anotaciones (regla ArchUnit). */
@Entity
@Table(name = "users")
class UserAccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String username;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccountEntity() {}

    static UserAccountEntity fromDomain(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.id = account.id();
        entity.username = account.username();
        entity.email = account.email();
        entity.passwordHash = account.passwordHash();
        entity.emailVerified = account.emailVerified();
        entity.role = account.role();
        entity.status = account.status();
        entity.createdAt = account.createdAt();
        entity.updatedAt = account.updatedAt();
        return entity;
    }

    UserAccount toDomain() {
        return UserAccount.restore(
                id, username, email, passwordHash, emailVerified, role, status, createdAt, updatedAt);
    }
}
