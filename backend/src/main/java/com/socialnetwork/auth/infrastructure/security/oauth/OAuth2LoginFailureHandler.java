package com.socialnetwork.auth.infrastructure.security.oauth;

import com.socialnetwork.auth.application.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/** Login social fallido (usuario canceló, error del proveedor): redirige al frontend con marca de error. */
@Component
class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    private final AuthProperties properties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    OAuth2LoginFailureHandler(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        log.warn("Fallo en el login social: {}", exception.getMessage());
        redirectStrategy.sendRedirect(
                request,
                response,
                properties.frontendBaseUrl() + properties.oauth().failurePath());
    }
}
