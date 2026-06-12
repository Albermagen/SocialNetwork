package com.socialnetwork.auth.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agregado raíz del módulo auth: identidad y credenciales de una cuenta. El perfil público
 * (avatar, bio, etc.) pertenece al módulo {@code user} (fase 2) y referencia este id.
 *
 * <p>Dominio puro: sin dependencias de framework (regla verificada por ArchUnit).
 */
public class UserAccount {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash; // null cuando la cuenta sea solo OAuth (iteración OAuth2)
    private boolean emailVerified;
    private final Role role;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserAccount(
            UUID id,
            String username,
            String email,
            String passwordHash,
            boolean emailVerified,
            Role role,
            AccountStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerified = emailVerified;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Alta por registro clásico (username + email + contraseña ya hasheada). */
    public static UserAccount register(UUID id, String username, String email, String passwordHash) {
        if (id == null) {
            throw new IllegalArgumentException("id requerido");
        }
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("username inválido: 3-30 caracteres [A-Za-z0-9_]");
        }
        if (email == null || !email.contains("@") || email.length() > 255) {
            throw new IllegalArgumentException("email inválido");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash requerido en el registro clásico");
        }
        Instant now = Instant.now();
        return new UserAccount(id, username, email, passwordHash, false, Role.USER, AccountStatus.ACTIVE, now, now);
    }

    /** Reconstrucción desde persistencia, sin validar invariantes de alta. */
    public static UserAccount restore(
            UUID id,
            String username,
            String email,
            String passwordHash,
            boolean emailVerified,
            Role role,
            AccountStatus status,
            Instant createdAt,
            Instant updatedAt) {
        return new UserAccount(id, username, email, passwordHash, emailVerified, role, status, createdAt, updatedAt);
    }

    /** Idempotente: verificar dos veces no es un error (reenvíos de email, doble clic). */
    public void verifyEmail() {
        if (!emailVerified) {
            this.emailVerified = true;
            touch();
        }
    }

    /** Solo las cuentas activas pueden autenticarse; suspendidas y borradas no. */
    public boolean canAuthenticate() {
        return status == AccountStatus.ACTIVE;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public Role role() {
        return role;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
