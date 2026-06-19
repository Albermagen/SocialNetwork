package com.socialnetwork.auth.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Reto MFA de vida corta entre el primer paso del login (contraseña correcta) y el segundo (código
 * TOTP). Adaptador Redis en {@code infrastructure.redis}. Token opaco de un solo uso.
 */
public interface MfaChallengeStore {

    /** Emite un reto para el usuario y devuelve el token en claro (se entrega en cookie httpOnly). */
    String issue(UUID userId);

    /** Devuelve el usuario del reto sin invalidarlo (permite reintentar un código mal tecleado). */
    Optional<UUID> resolve(String challengeToken);

    /** Invalida el reto (tras un segundo factor correcto). Idempotente. */
    void invalidate(String challengeToken);
}
