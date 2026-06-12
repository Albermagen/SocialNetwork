/**
 * Módulo `search`. Solo expone su paquete `api` al resto de módulos;
 * el resto del contenido es interno y su acceso se verifica en CI.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"catalog :: api", "user :: api", "social :: api"},
        displayName = "Search")
package com.socialnetwork.search;
