package com.socialnetwork.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos de la documentación OpenAPI (springdoc). UI en {@code /swagger-ui}. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SocialNetwork API")
                        .description("Plataforma social de entretenimiento: catálogo multimedia, "
                                + "listas, reviews y feed social.")
                        .version("v0"));
    }
}
