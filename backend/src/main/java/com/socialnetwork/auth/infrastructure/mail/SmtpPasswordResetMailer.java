package com.socialnetwork.auth.infrastructure.mail;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.PasswordResetMailer;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Envío real por SMTP (Mailpit en dev, proveedor transaccional en fase 10). */
class SmtpPasswordResetMailer implements PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    SmtpPasswordResetMailer(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendPasswordReset(String email, String username, String rawToken) {
        String link = properties.frontendBaseUrl() + "/reset-password?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@socialnetwork.local");
        message.setTo(email);
        message.setSubject("Restablece tu contraseña");
        message.setText(
                """
                Hola %s:

                Hemos recibido una solicitud para restablecer tu contraseña. Entra en:

                %s

                El enlace caduca en %d minutos. Si no fuiste tú, ignora este mensaje: tu contraseña
                no cambiará y tu cuenta sigue segura.
                """
                        .formatted(username, link, properties.reset().ttl().toMinutes()));
        mailSender.send(message);
    }
}
