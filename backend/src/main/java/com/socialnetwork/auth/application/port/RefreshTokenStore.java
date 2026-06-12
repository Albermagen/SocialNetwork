package com.socialnetwork.auth.application.port;

import java.util.UUID;

/**
 * Puerto del almacén de refresh tokens (rotación + detección de reuso por familia). Adaptador
 * Redis en {@code infrastructure.redis}.
 *
 * <p>Una <em>familia</em> agrupa la cadena de tokens rotados de una misma sesión. Si llega un
 * token ya rotado (posible robo), el adaptador revoca la familia completa.
 */
public interface RefreshTokenStore {

    /** Crea una familia nueva (login) y devuelve el token en claro. */
    String issue(UUID userId);

    /**
     * Rota el token: lo invalida y emite uno nuevo en la misma familia.
     *
     * @throws com.socialnetwork.shared.error.UnauthorizedException si el token es inválido o ya
     *     fue usado (en cuyo caso se revoca la familia entera).
     */
    Rotation rotate(String refreshToken);

    /** Revoca la familia a la que pertenece el token (logout). Ignora tokens desconocidos. */
    void revoke(String refreshToken);

    /** Revoca todas las familias del usuario (logout global). */
    void revokeAll(UUID userId);

    record Rotation(UUID userId, String newToken) {}
}
