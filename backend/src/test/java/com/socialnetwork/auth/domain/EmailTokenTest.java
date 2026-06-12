package com.socialnetwork.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.socialnetwork.shared.error.BusinessRuleException;
import com.socialnetwork.shared.util.Uuids;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EmailTokenTest {

    @Test
    void consumeMarksTokenAsUsed() {
        EmailToken token = verifyToken(Instant.now().plus(Duration.ofHours(1)));
        token.consume(Instant.now());
        assertThat(token.usedAt()).isNotNull();
    }

    @Test
    void consumeFailsWhenAlreadyUsed() {
        EmailToken token = verifyToken(Instant.now().plus(Duration.ofHours(1)));
        token.consume(Instant.now());
        assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> token.consume(Instant.now()));
    }

    @Test
    void consumeFailsWhenExpired() {
        EmailToken token = verifyToken(Instant.now().minus(Duration.ofMinutes(1)));
        assertThatExceptionOfType(BusinessRuleException.class).isThrownBy(() -> token.consume(Instant.now()));
    }

    private EmailToken verifyToken(Instant expiresAt) {
        return EmailToken.issue(
                Uuids.v7(),
                Uuids.v7(),
                SecureTokens.sha256Hex(SecureTokens.generate()),
                EmailTokenType.VERIFY,
                expiresAt);
    }
}
