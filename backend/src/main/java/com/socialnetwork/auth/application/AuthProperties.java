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
        @DefaultValue Reset reset,
        @DefaultValue Oauth oauth,
        @DefaultValue Mfa mfa,
        @DefaultValue Cookies cookies) {

    /** Secret HMAC (>= 32 bytes). Vacío → clave efímera con warning (solo dev/test). */
    public record Jwt(@DefaultValue("") String secret, @DefaultValue("15m") Duration accessTtl) {}

    public record Refresh(@DefaultValue("30d") Duration ttl) {}

    public record Verification(@DefaultValue("24h") Duration ttl) {}

    /** Token de reset: TTL corto (ventana de exposición mínima). */
    public record Reset(@DefaultValue("1h") Duration ttl) {}

    /** Rutas del frontend a las que redirige el login social tras éxito o fallo (sobre {@code frontendBaseUrl}). */
    public record Oauth(
            @DefaultValue("/oauth/callback") String successPath,
            @DefaultValue("/login?error=oauth") String failurePath) {}

    /** Segundo factor TOTP: emisor mostrado en la app autenticadora, TTL del reto y nº de códigos de recuperación. */
    public record Mfa(
            @DefaultValue("SocialNetwork") String issuer,
            @DefaultValue("5m") Duration challengeTtl,
            @DefaultValue("10") int recoveryCodes) {}

    /** {@code secure=false} solo en dev (sin TLS local). */
    public record Cookies(@DefaultValue("true") boolean secure) {}
}
