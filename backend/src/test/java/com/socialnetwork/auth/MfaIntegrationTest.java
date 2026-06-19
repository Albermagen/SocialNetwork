package com.socialnetwork.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.socialnetwork.TestcontainersConfiguration;
import com.socialnetwork.auth.application.port.VerificationMailer;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
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
 * Flujo MFA TOTP completo contra Postgres y Redis reales (Testcontainers): alta, confirmación,
 * login en dos pasos (reto + código), uso de un código de recuperación y baja.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MfaIntegrationTest.RecordingMailer.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MfaIntegrationTest {

    @TestConfiguration
    static class RecordingMailer {
        static final AtomicReference<String> LAST_TOKEN = new AtomicReference<>();

        @Bean
        @Primary
        VerificationMailer recordingVerificationMailer() {
            return (email, username, rawToken) -> LAST_TOKEN.set(rawToken);
        }
    }

    private static final String USERNAME = "mfauser";
    private static final String EMAIL = "mfa@example.com";
    private static final String PASSWORD = "S3gura-y-larga!9";

    private static String accessToken;
    private static String secret;
    private static String firstRecoveryCode;

    @Autowired
    private MockMvc mvc;

    @Test
    @Order(1)
    void registerVerifyAndLogin() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}"
                                .formatted(USERNAME, EMAIL, PASSWORD)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(RecordingMailer.LAST_TOKEN.get())))
                .andExpect(status().isNoContent());

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andReturn();
        accessToken = login.getResponse().getCookie("access_token").getValue();
    }

    @Test
    @Order(2)
    void enrollAndConfirm() throws Exception {
        MvcResult enroll = mvc.perform(post("/api/auth/mfa/enroll").cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpAuthUri").value(org.hamcrest.Matchers.startsWith("otpauth://")))
                .andReturn();
        secret = com.jayway.jsonpath.JsonPath.read(enroll.getResponse().getContentAsString(), "$.secret");

        MvcResult confirm = mvc.perform(post("/api/auth/mfa/confirm")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(validTotp(secret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodes").isArray())
                .andReturn();
        firstRecoveryCode = com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(
                        confirm.getResponse().getContentAsString(), "$.recoveryCodes")
                .get(0);

        mvc.perform(get("/api/auth/mfa/status").cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @Order(3)
    void loginNowRequiresSecondFactor() throws Exception {
        MvcResult firstStep = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn();
        // primer paso no abre sesión
        assertThat(firstStep.getResponse().getCookie("access_token")).isNull();
        String mfaToken = firstStep.getResponse().getCookie("mfa_token").getValue();
        assertThat(mfaToken).isNotBlank();

        // código erróneo: 401 pero el reto sobrevive
        mvc.perform(post("/api/auth/login/mfa")
                        .cookie(new Cookie("mfa_token", mfaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult secondStep = mvc.perform(post("/api/auth/login/mfa")
                        .cookie(new Cookie("mfa_token", mfaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(validTotp(secret))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andReturn();
        assertThat(secondStep.getResponse().getCookie("access_token").getValue())
                .isNotBlank();
    }

    @Test
    @Order(4)
    void recoveryCodeWorksOnceAsSecondFactor() throws Exception {
        String mfaToken = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andReturn()
                .getResponse()
                .getCookie("mfa_token")
                .getValue();

        mvc.perform(post("/api/auth/login/mfa")
                        .cookie(new Cookie("mfa_token", mfaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(firstRecoveryCode)))
                .andExpect(status().isOk());

        // el mismo código de recuperación ya no sirve (un solo uso)
        String mfaToken2 = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andReturn()
                .getResponse()
                .getCookie("mfa_token")
                .getValue();
        mvc.perform(post("/api/auth/login/mfa")
                        .cookie(new Cookie("mfa_token", mfaToken2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(firstRecoveryCode)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void disableMfaRestoresSingleFactorLogin() throws Exception {
        mvc.perform(post("/api/auth/mfa/disable")
                        .cookie(new Cookie("access_token", accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\"}".formatted(validTotp(secret))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    private static String validTotp(String secret) throws Exception {
        SystemTimeProvider time = new SystemTimeProvider();
        return new DefaultCodeGenerator().generate(secret, Math.floorDiv(time.getTime(), 30));
    }
}
