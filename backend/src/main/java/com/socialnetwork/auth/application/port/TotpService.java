package com.socialnetwork.auth.application.port;

import java.util.List;

/**
 * Puerto de operaciones TOTP (RFC 6238). Aísla la librería concreta para poder sustituirla.
 * Adaptador por defecto en {@code infrastructure.security}.
 */
public interface TotpService {

    /** Genera un secreto compartido (Base32). */
    String generateSecret();

    /** Construye la URI {@code otpauth://} que el cliente convierte en QR. */
    String otpAuthUri(String secret, String accountName);

    /** Valida un código de 6 dígitos contra el secreto, con tolerancia de ventana. */
    boolean isValidCode(String secret, String code);

    /** Genera {@code count} códigos de recuperación legibles. */
    List<String> generateRecoveryCodes(int count);
}
