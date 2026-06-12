package com.socialnetwork.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.socialnetwork.TestcontainersConfiguration;
import com.socialnetwork.auth.application.port.VerificationMailer;
import jakarta.servlet.http.Cookie;
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
 * Flujo completo de la fase 1 contra Postgres y Redis reales (Testcontainers):
 * registro → verificación → login → /me → refresh con rotación y detección de reuso → logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AuthFlowIntegrationTest.RecordingMailer.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    /** Captura el token de verificación que normalmente viajaría por email. */
    @TestConfiguration
    static class RecordingMailer {
        static final AtomicReference<String> LAST_TOKEN = new AtomicReference<>();

        @Bean
        @Primary
        VerificationMailer recordingVerificationMailer() {
            return (email, username, rawToken) -> LAST_TOKEN.set(rawToken);
        }
    }

    private static final String USERNAME = "alberto";
    private static final String EMAIL = "alberto@example.com";
    private static final String PASSWORD = "S3gura-y-larga!";

    private static String accessToken;
    private static String refreshToken;

    @Autowired
    private MockMvc mvc;

    @Test
    @Order(1)
    void registerCreatesAccountAndSendsVerificationToken() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(USERNAME, EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(USERNAME));

        assertThat(RecordingMailer.LAST_TOKEN.get()).isNotBlank();
    }

    @Test
    @Order(2)
    void duplicateUsernameIsConflict() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(USERNAME, "otro@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("username_taken"));
    }

    @Test
    @Order(3)
    void loginBeforeVerificationIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("email_not_verified"));
    }

    @Test
    @Order(4)
    void verifyEmailConsumesToken() throws Exception {
        String token = RecordingMailer.LAST_TOKEN.get();
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());

        // el token es de un solo uso
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(token)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(5)
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"incorrecta123\"}".formatted(USERNAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    @Order(6)
    void loginSetsHttpOnlyCookies() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andReturn();

        accessToken = result.getResponse().getCookie("access_token").getValue();
        refreshToken = result.getResponse().getCookie("refresh_token").getValue();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
    }

    @Test
    @Order(7)
    void meRequiresAndAcceptsAccessCookie() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/auth/me").cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @Order(8)
    void refreshRotatesTokenAndDetectsReuse() throws Exception {
        String oldRefresh = refreshToken;

        MvcResult result = mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", oldRefresh)))
                .andExpect(status().isNoContent())
                .andReturn();
        String rotated = result.getResponse().getCookie("refresh_token").getValue();
        assertThat(rotated).isNotBlank().isNotEqualTo(oldRefresh);

        // reuso del token ya rotado → 401 y revocación de la familia completa
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", oldRefresh)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", rotated)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(9)
    void logoutRevokesSession() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        String refresh = login.getResponse().getCookie("refresh_token").getValue();

        mvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", refresh)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refresh)))
                .andExpect(status().isUnauthorized());
    }

    private static String json(String username, String email, String password) {
        return "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(username, email, password);
    }
}
