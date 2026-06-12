package com.socialnetwork.architecture;

import com.socialnetwork.SocialNetworkApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifica las fronteras entre módulos (Spring Modulith): un módulo solo puede acceder a la API
 * pública de los módulos declarados en {@code allowedDependencies}. Falla la build si se viola.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(SocialNetworkApplication.class);

    @Test
    void verifyModuleBoundaries() {
        modules.verify();
    }

    /** Genera diagramas C4 y documentación de módulos en target/spring-modulith-docs. */
    @Test
    void writeDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
