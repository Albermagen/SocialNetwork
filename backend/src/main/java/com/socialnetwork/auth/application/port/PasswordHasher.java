package com.socialnetwork.auth.application.port;

/** Puerto de hashing de contraseñas. Adaptador BCrypt en {@code infrastructure.security}. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
