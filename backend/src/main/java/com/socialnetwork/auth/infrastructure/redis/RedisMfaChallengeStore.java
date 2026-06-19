package com.socialnetwork.auth.infrastructure.redis;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.MfaChallengeStore;
import com.socialnetwork.auth.domain.SecureTokens;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Reto MFA en Redis. Solo se guarda el hash del token (clave {@code auth:mfa:{hash}} → userId) con
 * TTL corto. El consumo elimina la clave (un solo uso); el {@code getAndDelete} es atómico.
 */
@Component
class RedisMfaChallengeStore implements MfaChallengeStore {

    private static final String PREFIX = "auth:mfa:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    RedisMfaChallengeStore(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.ttl = properties.mfa().challengeTtl();
    }

    @Override
    public String issue(UUID userId) {
        String token = SecureTokens.generate();
        redis.opsForValue().set(PREFIX + SecureTokens.sha256Hex(token), userId.toString(), ttl);
        return token;
    }

    @Override
    public Optional<UUID> resolve(String challengeToken) {
        if (challengeToken == null || challengeToken.isBlank()) {
            return Optional.empty();
        }
        String value = redis.opsForValue().get(PREFIX + SecureTokens.sha256Hex(challengeToken));
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    @Override
    public void invalidate(String challengeToken) {
        if (challengeToken != null && !challengeToken.isBlank()) {
            redis.delete(PREFIX + SecureTokens.sha256Hex(challengeToken));
        }
    }
}
