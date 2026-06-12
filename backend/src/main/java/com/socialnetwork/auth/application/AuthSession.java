package com.socialnetwork.auth.application;

import java.time.Duration;

/** Resultado de login/refresh: tokens y sus TTLs (la capa web los convierte en cookies). */
public record AuthSession(
        String accessToken, Duration accessTtl, String refreshToken, Duration refreshTtl, UserSnapshot user) {}
