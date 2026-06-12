package com.socialnetwork;

import org.springframework.boot.SpringApplication;
import org.springframework.modulith.Modulith;

/**
 * Monolito modular (Spring Modulith). Cada subpaquete directo es un módulo con fronteras
 * verificadas en CI. El módulo {@code shared} es kernel compartido, accesible por todos.
 */
@Modulith(systemName = "SocialNetwork", sharedModules = "shared")
public class SocialNetworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialNetworkApplication.class, args);
    }
}
