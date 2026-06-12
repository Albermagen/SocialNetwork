package com.socialnetwork.auth.api;

import java.util.UUID;

/**
 * Evento de dominio: una cuenta nueva se ha registrado. El módulo {@code user} (fase 2) lo
 * escuchará para crear el perfil inicial.
 */
public record UserRegistered(UUID userId, String username) {}
