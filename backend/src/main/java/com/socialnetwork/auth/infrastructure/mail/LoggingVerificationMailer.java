package com.socialnetwork.auth.infrastructure.mail;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.VerificationMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fallback sin SMTP configurado: escribe el enlace en el log (útil en tests y arranques mínimos). */
class LoggingVerificationMailer implements VerificationMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingVerificationMailer.class);

    private final AuthProperties properties;

    LoggingVerificationMailer(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendVerification(String email, String username, String rawToken) {
        log.info(
                "SMTP no configurado. Enlace de verificación para {} <{}>: {}/verify-email?token={}",
                username,
                email,
                properties.frontendBaseUrl(),
                rawToken);
    }
}
