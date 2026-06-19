package com.socialnetwork.auth.application;

import com.socialnetwork.auth.application.port.PasswordHasher;
import com.socialnetwork.auth.application.port.PasswordResetMailer;
import com.socialnetwork.auth.application.port.RefreshTokenStore;
import com.socialnetwork.auth.domain.EmailToken;
import com.socialnetwork.auth.domain.EmailTokenRepository;
import com.socialnetwork.auth.domain.EmailTokenType;
import com.socialnetwork.auth.domain.SecureTokens;
import com.socialnetwork.auth.domain.UserAccount;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.BusinessRuleException;
import com.socialnetwork.shared.util.Uuids;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de recuperación de contraseña.
 *
 * <p>Diseño de seguridad (OWASP): la solicitud responde siempre de forma neutra (no revela si el
 * email existe), el token es opaco y de un solo uso (solo se guarda su hash), tiene TTL corto y al
 * completar el reset se revocan todas las sesiones activas del usuario.
 */
@Service
public class PasswordResetService {

    private final UserAccountRepository users;
    private final EmailTokenRepository emailTokens;
    private final PasswordHasher passwordHasher;
    private final PasswordResetMailer mailer;
    private final RefreshTokenStore refreshTokens;
    private final AuthProperties properties;

    public PasswordResetService(
            UserAccountRepository users,
            EmailTokenRepository emailTokens,
            PasswordHasher passwordHasher,
            PasswordResetMailer mailer,
            RefreshTokenStore refreshTokens,
            AuthProperties properties) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.passwordHasher = passwordHasher;
        this.mailer = mailer;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
    }

    /** Solicita el reset. Sin efectos observables si el email no existe o la cuenta no puede autenticarse. */
    @Transactional
    public void requestReset(String email) {
        users.findByEmail(email).filter(UserAccount::canAuthenticate).ifPresent(this::issueResetToken);
    }

    /** Consume el token, fija la nueva contraseña y cierra todas las sesiones del usuario. */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        var token = emailTokens
                .findByTokenHashAndType(SecureTokens.sha256Hex(rawToken), EmailTokenType.RESET)
                .orElseThrow(() -> new BusinessRuleException("invalid_token", "El token no es válido o ha caducado"));
        token.consume(Instant.now());
        emailTokens.save(token);

        var account = users.findById(token.userId())
                .orElseThrow(() -> new BusinessRuleException("invalid_token", "El token no es válido o ha caducado"));
        account.changePassword(passwordHasher.hash(newPassword));
        users.save(account);

        // Cambiar la contraseña invalida cualquier sesión existente (posible compromiso).
        refreshTokens.revokeAll(account.id());
    }

    private void issueResetToken(UserAccount account) {
        emailTokens.deleteByUserIdAndType(account.id(), EmailTokenType.RESET);
        String rawToken = SecureTokens.generate();
        EmailToken token = EmailToken.issue(
                Uuids.v7(),
                account.id(),
                SecureTokens.sha256Hex(rawToken),
                EmailTokenType.RESET,
                Instant.now().plus(properties.reset().ttl()));
        emailTokens.save(token);
        mailer.sendPasswordReset(account.email(), account.username(), rawToken);
    }
}
