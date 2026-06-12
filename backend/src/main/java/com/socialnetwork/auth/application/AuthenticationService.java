package com.socialnetwork.auth.application;

import com.socialnetwork.auth.application.port.AccessTokenIssuer;
import com.socialnetwork.auth.application.port.PasswordHasher;
import com.socialnetwork.auth.application.port.RefreshTokenStore;
import com.socialnetwork.auth.domain.UserAccount;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.ForbiddenException;
import com.socialnetwork.shared.error.UnauthorizedException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de sesión: login, refresh con rotación, logout y logout global. */
@Service
public class AuthenticationService {

    private final UserAccountRepository users;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenStore refreshTokens;
    private final AuthProperties properties;

    public AuthenticationService(
            UserAccountRepository users,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokens,
            RefreshTokenStore refreshTokens,
            AuthProperties properties) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public AuthSession login(String identifier, String password) {
        var account = users.findByIdentifier(identifier).orElseThrow(this::invalidCredentials);
        if (account.passwordHash() == null || !passwordHasher.matches(password, account.passwordHash())) {
            throw invalidCredentials();
        }
        if (!account.canAuthenticate()) {
            throw new ForbiddenException("account_disabled", "La cuenta no está activa");
        }
        if (!account.emailVerified()) {
            throw new ForbiddenException("email_not_verified", "Verifica tu email antes de iniciar sesión");
        }
        return session(account, refreshTokens.issue(account.id()));
    }

    /** Rota el refresh token y emite un access token nuevo. */
    @Transactional(readOnly = true)
    public AuthSession refresh(String refreshToken) {
        var rotation = refreshTokens.rotate(refreshToken);
        var account = users.findById(rotation.userId())
                .filter(UserAccount::canAuthenticate)
                .orElseThrow(() -> new UnauthorizedException("invalid_refresh_token", "Sesión no válida"));
        return session(account, rotation.newToken());
    }

    /** Cierra la sesión actual (revoca la familia del refresh token). Idempotente. */
    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    /** Cierra todas las sesiones del usuario en todos los dispositivos. */
    public void logoutAll(UUID userId) {
        refreshTokens.revokeAll(userId);
    }

    private AuthSession session(UserAccount account, String refreshToken) {
        var snapshot = UserSnapshot.of(account);
        return new AuthSession(
                accessTokens.issue(snapshot),
                properties.jwt().accessTtl(),
                refreshToken,
                properties.refresh().ttl(),
                snapshot);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("invalid_credentials", "Credenciales incorrectas");
    }
}
