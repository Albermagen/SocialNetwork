package com.socialnetwork.auth.infrastructure.mail;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.PasswordResetMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fallback sin SMTP configurado: escribe el enlace en el log (útil en tests y arranques mínimos). */
class LoggingPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);

    private final AuthProperties properties;

    LoggingPasswordResetMailer(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendPasswordReset(String email, String username, String rawToken) {
        log.info(
                "SMTP no configurado. Enlace de reset para {} <{}>: {}/reset-password?token={}",
                username,
                email,
                properties.frontendBaseUrl(),
                rawToken);
    }
}
