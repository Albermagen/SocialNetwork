package com.socialnetwork.auth.application.port;

/**
 * Puerto de envío de emails de verificación. Adaptador SMTP (Mailpit en dev) en
 * {@code infrastructure.mail}; sin SMTP configurado, un adaptador de logging.
 */
@FunctionalInterface
public interface VerificationMailer {

    void sendVerification(String email, String username, String rawToken);
}
