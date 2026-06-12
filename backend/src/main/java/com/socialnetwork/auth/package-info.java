/**
 * Módulo `auth`. Solo expone su paquete `api` al resto de módulos;
 * el resto del contenido es interno y su acceso se verifica en CI.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {},
        displayName = "Auth")
package com.socialnetwork.auth;
