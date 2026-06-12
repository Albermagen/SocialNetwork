package com.socialnetwork.auth.web.dto;

import com.socialnetwork.auth.application.UserSnapshot;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public record UserResponse(UUID id, String username, String email, boolean emailVerified, String role) {

    public static UserResponse of(UserSnapshot user) {
        return new UserResponse(user.id(), user.username(), user.email(), user.emailVerified(), user.role());
    }

    /** Para {@code /me}: se construye desde los claims del access token, sin tocar la BD. */
    public static UserResponse of(Jwt jwt) {
        return new UserResponse(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("email"),
                true,
                jwt.getClaimAsString("role"));
    }
}
