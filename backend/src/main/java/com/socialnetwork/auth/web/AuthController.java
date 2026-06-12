package com.socialnetwork.auth.web;

import com.socialnetwork.auth.application.AuthSession;
import com.socialnetwork.auth.application.AuthenticationService;
import com.socialnetwork.auth.application.EmailVerificationService;
import com.socialnetwork.auth.application.RegisterCommand;
import com.socialnetwork.auth.application.RegistrationService;
import com.socialnetwork.auth.web.dto.LoginRequest;
import com.socialnetwork.auth.web.dto.RegisterRequest;
import com.socialnetwork.auth.web.dto.ResendVerificationRequest;
import com.socialnetwork.auth.web.dto.UserResponse;
import com.socialnetwork.auth.web.dto.VerifyEmailRequest;
import com.socialnetwork.shared.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de autenticación. Sin lógica de negocio: delega en los casos de uso y gestiona cookies. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationService authenticationService;
    private final AuthCookies cookies;

    AuthController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            AuthenticationService authenticationService,
            AuthCookies cookies) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationService = authenticationService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        UUID id = registrationService.register(
                new RegisterCommand(request.username(), request.email(), request.password()));
        return Map.of("id", id, "username", request.username(), "email", request.email());
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
    }

    /** 202 siempre: no revela si el email existe. */
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resendVerification(request.email());
    }

    @PostMapping("/login")
    UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthSession session = authenticationService.login(request.identifier(), request.password());
        cookies.write(response, session);
        return UserResponse.of(session.user());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void refresh(
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("missing_refresh_token", "No hay sesión que renovar");
        }
        AuthSession session = authenticationService.refresh(refreshToken);
        cookies.write(response, session);
    }

    /** Idempotente: sin cookie o con token inválido también responde 204 y limpia cookies. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authenticationService.logout(refreshToken);
        }
        cookies.clear(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logoutAll(@AuthenticationPrincipal Jwt jwt, HttpServletResponse response) {
        authenticationService.logoutAll(UUID.fromString(jwt.getSubject()));
        cookies.clear(response);
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.of(jwt);
    }
}
