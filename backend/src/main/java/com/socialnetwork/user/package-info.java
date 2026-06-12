/**
 * Módulo `user`. Solo expone su paquete `api` al resto de módulos;
 * el resto del contenido es interno y su acceso se verifica en CI.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"auth :: api"},
        displayName = "User")
package com.socialnetwork.user;
