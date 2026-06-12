package com.socialnetwork.auth.application;

import com.socialnetwork.auth.api.UserRegistered;
import com.socialnetwork.auth.application.port.PasswordHasher;
import com.socialnetwork.auth.application.port.VerificationMailer;
import com.socialnetwork.auth.domain.EmailToken;
import com.socialnetwork.auth.domain.EmailTokenRepository;
import com.socialnetwork.auth.domain.EmailTokenType;
import com.socialnetwork.auth.domain.SecureTokens;
import com.socialnetwork.auth.domain.UserAccount;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.ConflictException;
import com.socialnetwork.shared.util.Uuids;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: registro de cuenta + emisión del token de verificación por email. */
@Service
public class RegistrationService {

    private final UserAccountRepository users;
    private final EmailTokenRepository emailTokens;
    private final PasswordHasher passwordHasher;
    private final VerificationMailer mailer;
    private final ApplicationEventPublisher events;
    private final AuthProperties properties;

    public RegistrationService(
            UserAccountRepository users,
            EmailTokenRepository emailTokens,
            PasswordHasher passwordHasher,
            VerificationMailer mailer,
            ApplicationEventPublisher events,
            AuthProperties properties) {
        this.users = users;
        this.emailTokens = emailTokens;
        this.passwordHasher = passwordHasher;
        this.mailer = mailer;
        this.events = events;
        this.properties = properties;
    }

    @Transactional
    public UUID register(RegisterCommand command) {
        if (users.existsByUsername(command.username())) {
            throw new ConflictException("username_taken", "El nombre de usuario ya está en uso");
        }
        if (users.existsByEmail(command.email())) {
            throw new ConflictException("email_taken", "Ya existe una cuenta con ese email");
        }
        UserAccount account = UserAccount.register(
                Uuids.v7(), command.username(), command.email(), passwordHasher.hash(command.password()));
        users.save(account);
        sendVerificationToken(account);
        events.publishEvent(new UserRegistered(account.id(), account.username()));
        return account.id();
    }

    /**
     * Reenvío del email de verificación. Respuesta neutra siempre: no revela si el email existe
     * (OWASP, enumeración de cuentas). El rate limiting protege contra abuso.
     */
    @Transactional
    public void resendVerification(String email) {
        users.findByEmail(email).filter(account -> !account.emailVerified()).ifPresent(this::sendVerificationToken);
    }

    private void sendVerificationToken(UserAccount account) {
        String rawToken = SecureTokens.generate();
        EmailToken token = EmailToken.issue(
                Uuids.v7(),
                account.id(),
                SecureTokens.sha256Hex(rawToken),
                EmailTokenType.VERIFY,
                Instant.now().plus(properties.verification().ttl()));
        emailTokens.save(token);
        mailer.sendVerification(account.email(), account.username(), rawToken);
    }
}
