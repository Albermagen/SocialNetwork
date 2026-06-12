package com.socialnetwork.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code identifier} admite username o email. */
public record LoginRequest(@NotBlank @Size(max = 255) String identifier, @NotBlank @Size(max = 72) String password) {}
