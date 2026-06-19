package com.socialnetwork.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 128) String token,
        // máx. 72: límite de entrada de BCrypt
        @NotBlank @Size(min = 8, max = 72) String newPassword) {}
