package com.socialnetwork.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "3-30 caracteres: letras, números y _") String username,
        @NotBlank @Email @Size(max = 255) String email,
        // máx. 72: límite de entrada de BCrypt
        @NotBlank @Size(min = 8, max = 72) String password) {}
