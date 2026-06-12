package com.socialnetwork;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Smoke test de integración: el contexto arranca completo contra Postgres y Redis reales
 * (Testcontainers), incluyendo las migraciones Flyway.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SocialNetworkApplicationTests {

    @Test
    void contextLoads() {}
}
