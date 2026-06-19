package com.socialnetwork.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Código de verificación: TOTP de 6 dígitos o un código de recuperación. */
public record MfaCodeRequest(@NotBlank @Size(max = 32) String code) {}
