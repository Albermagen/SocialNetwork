package com.socialnetwork.auth.web;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.AuthSession;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookies httpOnly de sesión. El access va con {@code Path=/} y SameSite=Lax; el refresh queda
 * confinado a {@code Path=/api/auth} con SameSite=Strict (solo lo ven refresh/logout, nunca el
 * resto de la API ni el JS del navegador).
 */
@Component
public class AuthCookies {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    static final String REFRESH_PATH = "/api/auth";

    private final AuthProperties properties;

    AuthCookies(AuthProperties properties) {
        this.properties = properties;
    }

    void write(HttpServletResponse response, AuthSession session) {
        add(response, accessCookie(session.accessToken(), session.accessTtl()));
        add(response, refreshCookie(session.refreshToken(), session.refreshTtl()));
    }

    void clear(HttpServletResponse response) {
        add(response, accessCookie("", Duration.ZERO));
        add(response, refreshCookie("", Duration.ZERO));
    }

    private ResponseCookie accessCookie(String value, Duration maxAge) {
        return ResponseCookie.from(ACCESS_TOKEN, value)
                .httpOnly(true)
                .secure(properties.cookies().secure())
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN, value)
                .httpOnly(true)
                .secure(properties.cookies().secure())
                .path(REFRESH_PATH)
                .sameSite("Strict")
                .maxAge(maxAge)
                .build();
    }

    private void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
