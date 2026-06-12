package com.socialnetwork.auth.application;

import com.socialnetwork.auth.domain.EmailTokenRepository;
import com.socialnetwork.auth.domain.EmailTokenType;
import com.socialnetwork.auth.domain.SecureTokens;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.BusinessRuleException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: consumir el token recibido por email y marcar la cuenta como verificada. */
@Service
public class EmailVerificationService {

    private final EmailTokenRepository emailTokens;
    private final UserAccountRepository users;

    public EmailVerificationService(EmailTokenRepository emailTokens, UserAccountRepository users) {
        this.emailTokens = emailTokens;
        this.users = users;
    }

    @Transactional
    public void verify(String rawToken) {
        var token = emailTokens
                .findByTokenHashAndType(SecureTokens.sha256Hex(rawToken), EmailTokenType.VERIFY)
                .orElseThrow(() -> new BusinessRuleException("invalid_token", "El token no es válido o ha caducado"));
        token.consume(Instant.now());
        emailTokens.save(token);

        var account = users.findById(token.userId())
                .orElseThrow(() -> new BusinessRuleException("invalid_token", "El token no es válido o ha caducado"));
        account.verifyEmail();
        users.save(account);
    }
}
