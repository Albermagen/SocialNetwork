package com.socialnetwork.auth.web.dto;

/** Estado del segundo factor para el usuario autenticado. */
public record MfaStatusResponse(boolean enabled) {}
