package com.socialnetwork.auth.infrastructure.mail;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.VerificationMailer;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Envío real por SMTP (Mailpit en dev, proveedor transaccional en fase 10). */
class SmtpVerificationMailer implements VerificationMailer {

    private final JavaMailSender mailSender;
    private final AuthProperties properties;

    SmtpVerificationMailer(JavaMailSender mailSender, AuthProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendVerification(String email, String username, String rawToken) {
        String link = properties.frontendBaseUrl() + "/verify-email?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@socialnetwork.local");
        message.setTo(email);
        message.setSubject("Verifica tu cuenta");
        message.setText(
                """
                Hola %s:

                Confirma tu dirección de email entrando en:

                %s

                El enlace caduca en %d horas. Si no creaste esta cuenta, ignora este mensaje.
                """
                        .formatted(
                                username, link, properties.verification().ttl().toHours()));
        mailSender.send(message);
    }
}
