/**
 * Módulo `social`. Solo expone su paquete `api` al resto de módulos;
 * el resto del contenido es interno y su acceso se verifica en CI.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user :: api"},
        displayName = "Social")
package com.socialnetwork.social;
