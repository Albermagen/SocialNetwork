package com.socialnetwork.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.socialnetwork.TestcontainersConfiguration;
import com.socialnetwork.auth.application.port.PasswordResetMailer;
import com.socialnetwork.auth.application.port.VerificationMailer;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Flujo de recuperación de contraseña contra Postgres y Redis reales (Testcontainers):
 * registro → verificación → forgot-password → reset-password → login con la nueva contraseña.
 * Cubre token de un solo uso, respuesta neutra anti-enumeración e invalidación de la antigua.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, PasswordResetIntegrationTest.RecordingMailers.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PasswordResetIntegrationTest {

    /** Captura los tokens que normalmente viajarían por email. */
    @TestConfiguration
    static class RecordingMailers {
        static final AtomicReference<String> LAST_VERIFICATION = new AtomicReference<>();
        static final AtomicReference<String> LAST_RESET = new AtomicReference<>();

        @Bean
        @Primary
        VerificationMailer recordingVerificationMailer() {
            return (email, username, rawToken) -> LAST_VERIFICATION.set(rawToken);
        }

        @Bean
        @Primary
        PasswordResetMailer recordingPasswordResetMailer() {
            return (email, username, rawToken) -> LAST_RESET.set(rawToken);
        }
    }

    private static final String USERNAME = "resetuser";
    private static final String EMAIL = "reset@example.com";
    private static final String OLD_PASSWORD = "Antigua-y-larga!1";
    private static final String NEW_PASSWORD = "Nueva-y-mas-larga!2";

    @Autowired
    private MockMvc mvc;

    @Test
    @Order(1)
    void registerAndVerify() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(USERNAME, EMAIL, OLD_PASSWORD)))
                .andExpect(status().isCreated());

        String token = RecordingMailers.LAST_VERIFICATION.get();
        assertThat(token).isNotBlank();
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(2)
    void forgotPasswordIsAlwaysAcceptedAndNeutral() throws Exception {
        // email existente
        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(EMAIL)))
                .andExpect(status().isAccepted());
        assertThat(RecordingMailers.LAST_RESET.get()).isNotBlank();

        // email inexistente: misma respuesta, sin filtrar existencia
        mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noexiste@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    @Order(3)
    void resetPasswordChangesCredentialAndIsSingleUse() throws Exception {
        String token = RecordingMailers.LAST_RESET.get();

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // el token es de un solo uso
        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, NEW_PASSWORD)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(4)
    void oldPasswordRejectedAndNewPasswordWorks() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, OLD_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));

        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("access_token", true))
                .andReturn();
        assertThat(result.getResponse().getCookie("access_token").getValue()).isNotBlank();
    }

    @Test
    @Order(5)
    void invalidResetTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-inexistente\",\"newPassword\":\"%s\"}".formatted(NEW_PASSWORD)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("invalid_token"));
    }
}
