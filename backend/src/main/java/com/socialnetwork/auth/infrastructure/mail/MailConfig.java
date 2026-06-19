package com.socialnetwork.auth.infrastructure.mail;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.PasswordResetMailer;
import com.socialnetwork.auth.application.port.VerificationMailer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Selección del adaptador de correo: SMTP si hay {@code spring.mail.host}, logging si no. */
@Configuration
class MailConfig {

    @Bean
    @ConditionalOnProperty("spring.mail.host")
    VerificationMailer smtpVerificationMailer(JavaMailSender mailSender, AuthProperties properties) {
        return new SmtpVerificationMailer(mailSender, properties);
    }

    @Bean
    @ConditionalOnMissingBean(VerificationMailer.class)
    VerificationMailer loggingVerificationMailer(AuthProperties properties) {
        return new LoggingVerificationMailer(properties);
    }

    @Bean
    @ConditionalOnProperty("spring.mail.host")
    PasswordResetMailer smtpPasswordResetMailer(JavaMailSender mailSender, AuthProperties properties) {
        return new SmtpPasswordResetMailer(mailSender, properties);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordResetMailer.class)
    PasswordResetMailer loggingPasswordResetMailer(AuthProperties properties) {
        return new LoggingPasswordResetMailer(properties);
    }
}
