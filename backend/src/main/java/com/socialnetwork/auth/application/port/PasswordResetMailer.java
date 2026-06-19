package com.socialnetwork.auth.application.port;

/**
 * Puerto de envío de emails de recuperación de contraseña. Adaptador SMTP (Mailpit en dev) en
 * {@code infrastructure.mail}; sin SMTP configurado, un adaptador de logging.
 */
@FunctionalInterface
public interface PasswordResetMailer {

    void sendPasswordReset(String email, String username, String rawToken);
}
