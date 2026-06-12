package com.socialnetwork.auth.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del módulo auth ({@code app.auth.*}). Registrada con
 * {@code @EnableConfigurationProperties} en {@code SecurityConfig}.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @DefaultValue("http://localhost:3000") String frontendBaseUrl,
        @DefaultValue Jwt jwt,
        @DefaultValue Refresh refresh,
        @DefaultValue Verification verification,
        @DefaultValue Cookies cookies) {

    /** Secret HMAC (>= 32 bytes). Vacío → clave efímera con warning (solo dev/test). */
    public record Jwt(@DefaultValue("") String secret, @DefaultValue("15m") Duration accessTtl) {}

    public record Refresh(@DefaultValue("30d") Duration ttl) {}

    public record Verification(@DefaultValue("24h") Duration ttl) {}

    /** {@code secure=false} solo en dev (sin TLS local). */
    public record Cookies(@DefaultValue("true") boolean secure) {}
}
