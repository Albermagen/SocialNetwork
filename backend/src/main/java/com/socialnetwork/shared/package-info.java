/**
 * Kernel compartido: tipos de valor comunes, jerarquía de errores y configuración transversal.
 * Módulo abierto (sharedModules en {@link com.socialnetwork.SocialNetworkApplication}).
 * Mantener al mínimo: nada de lógica de negocio aquí.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Shared Kernel")
package com.socialnetwork.shared;
