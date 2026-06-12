package com.socialnetwork.auth.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.UserSnapshot;
import com.socialnetwork.auth.application.port.AccessTokenIssuer;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Emisión de access tokens JWT firmados con HMAC-SHA256. Simétrico es suficiente mientras emisor y
 * validador son el mismo monolito; al extraer servicios (fase 12) se migrará a RSA/EdDSA con JWKS.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ISSUER = "socialnetwork";

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    JwtAccessTokenIssuer(SecretKey jwtSecretKey, AuthProperties properties) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
        this.properties = properties;
    }

    @Override
    public String issue(UserSnapshot user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().accessTtl()))
                .claim("username", user.username())
                .claim("email", user.email())
                .claim("role", user.role())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
