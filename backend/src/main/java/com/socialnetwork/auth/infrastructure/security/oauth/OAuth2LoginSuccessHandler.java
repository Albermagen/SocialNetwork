package com.socialnetwork.auth.infrastructure.security.oauth;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.AuthSession;
import com.socialnetwork.auth.application.AuthenticationService;
import com.socialnetwork.auth.application.OauthLoginService;
import com.socialnetwork.auth.domain.OauthProvider;
import com.socialnetwork.auth.web.AuthCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Tras un login OAuth2 correcto: resuelve/crea la cuenta local, emite nuestras cookies de sesión
 * (access + refresh) y redirige al frontend. Sustituye por completo el comportamiento por defecto
 * de Spring (que dejaría una sesión de servidor que aquí no usamos).
 */
@Component
class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final OauthLoginService oauthLoginService;
    private final AuthenticationService authenticationService;
    private final AuthCookies cookies;
    private final AuthProperties properties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    OAuth2LoginSuccessHandler(
            OauthLoginService oauthLoginService,
            AuthenticationService authenticationService,
            AuthCookies cookies,
            AuthProperties properties) {
        this.oauthLoginService = oauthLoginService;
        this.authenticationService = authenticationService;
        this.cookies = cookies;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        try {
            OAuth2User principal = (OAuth2User) authentication.getPrincipal();
            String providerUserId = claim(principal, "sub");
            String email = claim(principal, "email");
            boolean emailVerified = principal instanceof OidcUser oidc
                    ? Boolean.TRUE.equals(oidc.getEmailVerified())
                    : Boolean.parseBoolean(claim(principal, "email_verified"));
            String name = claim(principal, "name");

            UUID userId =
                    oauthLoginService.resolveAccount(OauthProvider.GOOGLE, providerUserId, email, emailVerified, name);
            AuthSession session = authenticationService.startSession(userId);
            cookies.write(response, session);
            redirectStrategy.sendRedirect(
                    request,
                    response,
                    properties.frontendBaseUrl() + properties.oauth().successPath());
        } catch (RuntimeException ex) {
            log.warn("Login social rechazado: {}", ex.getMessage());
            redirectStrategy.sendRedirect(
                    request,
                    response,
                    properties.frontendBaseUrl() + properties.oauth().failurePath());
        }
    }

    private static String claim(OAuth2User principal, String name) {
        Object value = principal.getAttribute(name);
        return value != null ? value.toString() : null;
    }
}
