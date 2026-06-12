package com.socialnetwork.auth.application;

import com.socialnetwork.auth.domain.UserAccount;
import java.util.UUID;

/** Proyección inmutable de la cuenta para emitir tokens y responder al cliente. */
public record UserSnapshot(UUID id, String username, String email, boolean emailVerified, String role) {

    public static UserSnapshot of(UserAccount account) {
        return new UserSnapshot(
                account.id(),
                account.username(),
                account.email(),
                account.emailVerified(),
                account.role().name());
    }
}
