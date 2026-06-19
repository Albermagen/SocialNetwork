package com.socialnetwork.auth.web;

import com.socialnetwork.auth.application.AuthSession;
import com.socialnetwork.auth.application.AuthenticationService;
import com.socialnetwork.auth.application.EmailVerificationService;
import com.socialnetwork.auth.application.LoginOutcome;
import com.socialnetwork.auth.application.MfaService;
import com.socialnetwork.auth.application.PasswordResetService;
import com.socialnetwork.auth.application.RegisterCommand;
import com.socialnetwork.auth.application.RegistrationService;
import com.socialnetwork.auth.web.dto.ForgotPasswordRequest;
import com.socialnetwork.auth.web.dto.LoginRequest;
import com.socialnetwork.auth.web.dto.MfaCodeRequest;
import com.socialnetwork.auth.web.dto.MfaConfirmResponse;
import com.socialnetwork.auth.web.dto.MfaEnrollResponse;
import com.socialnetwork.auth.web.dto.MfaStatusResponse;
import com.socialnetwork.auth.web.dto.RegisterRequest;
import com.socialnetwork.auth.web.dto.ResendVerificationRequest;
import com.socialnetwork.auth.web.dto.ResetPasswordRequest;
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
    private final PasswordResetService passwordResetService;
    private final MfaService mfaService;
    private final AuthCookies cookies;

    AuthController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService,
            MfaService mfaService,
            AuthCookies cookies) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
        this.mfaService = mfaService;
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

    /** 202 siempre: no revela si el email existe (anti-enumeración). */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
    }

    /** Consume el token de reset y fija la nueva contraseña. Revoca todas las sesiones. */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
    }

    /**
     * Primer paso del login. Sin MFA: emite cookies de sesión y devuelve el usuario. Con MFA activo:
     * no abre sesión, deja un reto en cookie {@code mfa_token} y responde {@code {"mfaRequired":true}}.
     */
    @PostMapping("/login")
    Object login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginOutcome outcome = authenticationService.login(request.identifier(), request.password());
        if (outcome.mfaRequired()) {
            cookies.writeMfaChallenge(response, outcome.mfaToken(), outcome.mfaTtl());
            return Map.of("mfaRequired", true);
        }
        cookies.write(response, outcome.session());
        return UserResponse.of(outcome.session().user());
    }

    /** Segundo paso del login: valida el código (TOTP o recuperación) contra el reto en cookie. */
    @PostMapping("/login/mfa")
    UserResponse loginMfa(
            @CookieValue(name = AuthCookies.MFA_TOKEN, required = false) String mfaToken,
            @Valid @RequestBody MfaCodeRequest request,
            HttpServletResponse response) {
        if (mfaToken == null || mfaToken.isBlank()) {
            throw new UnauthorizedException("missing_mfa_challenge", "No hay un reto MFA en curso");
        }
        AuthSession session = authenticationService.completeMfaLogin(mfaToken, request.code());
        cookies.clearMfaChallenge(response);
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

    /** Inicia el alta de MFA: devuelve secreto y URI otpauth. Aún inactivo hasta confirmar. */
    @PostMapping("/mfa/enroll")
    MfaEnrollResponse enrollMfa(@AuthenticationPrincipal Jwt jwt) {
        return MfaEnrollResponse.of(mfaService.enroll(UUID.fromString(jwt.getSubject())));
    }

    /** Confirma el alta verificando el primer código; devuelve los códigos de recuperación una vez. */
    @PostMapping("/mfa/confirm")
    MfaConfirmResponse confirmMfa(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaCodeRequest request) {
        return new MfaConfirmResponse(mfaService.confirm(UUID.fromString(jwt.getSubject()), request.code()));
    }

    /** Desactiva el segundo factor (requiere un código válido). */
    @PostMapping("/mfa/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disableMfa(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaCodeRequest request) {
        mfaService.disable(UUID.fromString(jwt.getSubject()), request.code());
    }

    @GetMapping("/mfa/status")
    MfaStatusResponse mfaStatus(@AuthenticationPrincipal Jwt jwt) {
        return new MfaStatusResponse(mfaService.isEnabled(UUID.fromString(jwt.getSubject())));
    }
}
