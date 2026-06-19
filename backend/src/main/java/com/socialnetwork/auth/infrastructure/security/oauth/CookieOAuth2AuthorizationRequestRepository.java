package com.socialnetwork.auth.infrastructure.security.oauth;

import com.socialnetwork.auth.application.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Almacena el {@link OAuth2AuthorizationRequest} en una cookie en lugar de en la sesión HTTP (la
 * app es stateless). La cookie se firma con HMAC-SHA256 (misma clave que el JWT) y solo se
 * deserializa tras validar la firma: evita inyección de payloads de deserialización ajenos.
 *
 * <p>{@code SameSite=Lax} es obligatorio: el retorno desde Google es una navegación GET top-level
 * cross-site; con {@code Strict} el navegador no enviaría la cookie y el flujo fallaría.
 */
@Component
class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(CookieOAuth2AuthorizationRequestRepository.class);
    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final Duration TTL = Duration.ofMinutes(3);
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final SecretKey signingKey;
    private final AuthProperties properties;

    CookieOAuth2AuthorizationRequestRepository(SecretKey jwtSecretKey, AuthProperties properties) {
        this.signingKey = jwtSecretKey;
        this.properties = properties;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request).map(this::deserialize).orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }
        String value = serialize(authorizationRequest);
        ResponseCookie cookie = baseCookie(value, TTL).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(response);
        return authorizationRequest;
    }

    private java.util.Optional<String> readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return java.util.Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return java.util.Optional.ofNullable(cookie.getValue());
            }
        }
        return java.util.Optional.empty();
    }

    private void deleteCookie(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, baseCookie("", Duration.ZERO).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.cookies().secure())
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge);
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(authorizationRequest);
            out.flush();
            String data = ENCODER.encodeToString(bytes.toByteArray());
            return data + "." + ENCODER.encodeToString(hmac(data));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el authorization request", e);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String cookieValue) {
        int dot = cookieValue.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String data = cookieValue.substring(0, dot);
        byte[] presentedMac = decodeOrEmpty(cookieValue.substring(dot + 1));
        if (!MessageDigest.isEqual(hmac(data), presentedMac)) {
            log.warn("Cookie de authorization request con firma inválida: se ignora");
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(DECODER.decode(data)))) {
            return (OAuth2AuthorizationRequest) in.readObject();
        } catch (Exception e) {
            log.warn("No se pudo deserializar el authorization request", e);
            return null;
        }
    }

    private byte[] decodeOrEmpty(String value) {
        try {
            return DECODER.decode(value);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el HMAC de la cookie OAuth", e);
        }
    }
}
