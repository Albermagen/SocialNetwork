package com.socialnetwork.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de Clean Architecture dentro de cada módulo: web → application → domain;
 * infrastructure implementa puertos de application/domain. El dominio no conoce frameworks.
 */
@AnalyzeClasses(packages = "com.socialnetwork", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "jakarta.servlet..")
            .allowEmptyShould(true)
            .because("el dominio no debe conocer Spring ni JPA");

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..", "..web..")
            .allowEmptyShould(true)
            .because("la regla de dependencia apunta hacia dentro");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..web..")
            .allowEmptyShould(true)
            .because("los casos de uso dependen de puertos, no de adaptadores");

    @ArchTest
    static final ArchRule controllersLiveInWeb = classes()
            .that()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should()
            .resideInAPackage("..web..")
            .allowEmptyShould(true)
            .because("los controladores pertenecen a la capa web y nunca contienen lógica de negocio");

    @ArchTest
    static final ArchRule repositoriesLiveInInfrastructure = classes()
            .that()
            .areAssignableTo(org.springframework.data.repository.Repository.class)
            .should()
            .resideInAPackage("..infrastructure..")
            .allowEmptyShould(true)
            .because("los repositorios JPA son adaptadores de infraestructura");
}
