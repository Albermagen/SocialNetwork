package com.socialnetwork.auth.application;

import java.time.Duration;

/**
 * Resultado del primer paso del login: o bien una sesión ya emitida (sin MFA), o un reto pendiente
 * de segundo factor. La capa web traduce cada caso a cookies y cuerpo de respuesta.
 */
public record LoginOutcome(AuthSession session, String mfaToken, Duration mfaTtl) {

    public static LoginOutcome authenticated(AuthSession session) {
        return new LoginOutcome(session, null, null);
    }

    public static LoginOutcome mfaChallenge(String mfaToken, Duration mfaTtl) {
        return new LoginOutcome(null, mfaToken, mfaTtl);
    }

    public boolean mfaRequired() {
        return session == null;
    }
}
