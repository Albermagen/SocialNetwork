package com.socialnetwork.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.socialnetwork.shared.util.Uuids;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    void registerCreatesActiveUnverifiedUserAccount() {
        UserAccount account = UserAccount.register(Uuids.v7(), "alberto", "alberto@example.com", "$2a$12$hash");

        assertThat(account.emailVerified()).isFalse();
        assertThat(account.role()).isEqualTo(Role.USER);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.canAuthenticate()).isTrue();
    }

    @Test
    void registerRejectsInvalidUsername() {
        UUID id = Uuids.v7();
        assertThatIllegalArgumentException().isThrownBy(() -> UserAccount.register(id, "ab", "a@b.com", "hash"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UserAccount.register(id, "con espacios", "a@b.com", "hash"));
    }

    @Test
    void verifyEmailIsIdempotent() {
        UserAccount account = UserAccount.register(Uuids.v7(), "alberto", "alberto@example.com", "hash");
        account.verifyEmail();
        account.verifyEmail();
        assertThat(account.emailVerified()).isTrue();
    }

    @Test
    void suspendedAccountCannotAuthenticate() {
        UserAccount account = UserAccount.restore(
                Uuids.v7(),
                "alberto",
                "alberto@example.com",
                "hash",
                true,
                Role.USER,
                AccountStatus.SUSPENDED,
                Instant.now(),
                Instant.now());
        assertThat(account.canAuthenticate()).isFalse();
    }
}
