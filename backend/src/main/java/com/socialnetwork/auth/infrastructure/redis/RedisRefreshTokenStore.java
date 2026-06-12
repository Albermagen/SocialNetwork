package com.socialnetwork.auth.infrastructure.redis;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.RefreshTokenStore;
import com.socialnetwork.auth.domain.SecureTokens;
import com.socialnetwork.shared.error.UnauthorizedException;
import com.socialnetwork.shared.util.Uuids;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Refresh tokens opacos en Redis con TTL, rotación y detección de reuso.
 *
 * <p>Esquema de claves (solo se guardan hashes SHA-256, nunca el token en claro):
 *
 * <pre>
 * auth:rt:{hash}        → "userId:familyId"   token activo (TTL = ttl del refresh)
 * auth:rt:used:{hash}   → familyId            token ya rotado (para detectar reuso)
 * auth:fam:{familyId}   → SET de hashes       miembros de la familia (activos + usados)
 * auth:user:{userId}    → SET de familyIds    familias del usuario (logout global)
 * </pre>
 *
 * <p>Si llega un token presente en {@code used} (reuso → posible robo de sesión), se revoca la
 * familia completa. Las operaciones no son atómicas entre claves; una carrera en el peor caso
 * revoca de más (falla seguro). Mejora futura: script Lua.
 */
@Component
class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRefreshTokenStore.class);

    private static final String ACTIVE = "auth:rt:";
    private static final String USED = "auth:rt:used:";
    private static final String FAMILY = "auth:fam:";
    private static final String USER = "auth:user:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisRefreshTokenStore(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = properties.refresh().ttl();
    }

    @Override
    public String issue(UUID userId) {
        String familyId = Uuids.v7().toString();
        redis.opsForSet().add(USER + userId, familyId);
        redis.expire(USER + userId, ttl);
        return issueInFamily(userId.toString(), familyId);
    }

    @Override
    public Rotation rotate(String refreshToken) {
        String hash = SecureTokens.sha256Hex(refreshToken);
        String value = redis.opsForValue().get(ACTIVE + hash);
        if (value == null) {
            String familyId = redis.opsForValue().get(USED + hash);
            if (familyId != null) {
                log.warn("Reuso de refresh token detectado: se revoca la familia {}", familyId);
                revokeFamily(familyId);
            }
            throw new UnauthorizedException("invalid_refresh_token", "Sesión no válida");
        }
        int separator = value.indexOf(':');
        String userId = value.substring(0, separator);
        String familyId = value.substring(separator + 1);

        redis.delete(ACTIVE + hash);
        redis.opsForValue().set(USED + hash, familyId, ttl);
        String newToken = issueInFamily(userId, familyId);
        return new Rotation(UUID.fromString(userId), newToken);
    }

    @Override
    public void revoke(String refreshToken) {
        String hash = SecureTokens.sha256Hex(refreshToken);
        String value = redis.opsForValue().get(ACTIVE + hash);
        if (value != null) {
            revokeFamily(value.substring(value.indexOf(':') + 1));
        }
    }

    @Override
    public void revokeAll(UUID userId) {
        Set<String> families = redis.opsForSet().members(USER + userId);
        if (families != null) {
            families.forEach(this::revokeFamily);
        }
        redis.delete(USER + userId);
    }

    private String issueInFamily(String userId, String familyId) {
        String token = SecureTokens.generate();
        String hash = SecureTokens.sha256Hex(token);
        redis.opsForValue().set(ACTIVE + hash, userId + ":" + familyId, ttl);
        redis.opsForSet().add(FAMILY + familyId, hash);
        redis.expire(FAMILY + familyId, ttl);
        return token;
    }

    private void revokeFamily(String familyId) {
        Set<String> hashes = redis.opsForSet().members(FAMILY + familyId);
        if (hashes != null) {
            for (String hash : hashes) {
                redis.delete(ACTIVE + hash);
                redis.delete(USED + hash);
            }
        }
        redis.delete(FAMILY + familyId);
    }
}
