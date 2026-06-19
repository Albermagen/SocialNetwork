package com.socialnetwork.auth.application;

import com.socialnetwork.auth.application.port.TotpService;
import com.socialnetwork.auth.domain.MfaCredential;
import com.socialnetwork.auth.domain.MfaCredentialRepository;
import com.socialnetwork.auth.domain.MfaRecoveryCodeRepository;
import com.socialnetwork.auth.domain.SecureTokens;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.BusinessRuleException;
import com.socialnetwork.shared.error.ConflictException;
import com.socialnetwork.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del segundo factor TOTP: enrolamiento, confirmación, baja y verificación.
 *
 * <p>La verificación admite tanto un código TOTP como un código de recuperación (de un solo uso).
 * Los secretos y los códigos de recuperación nunca se devuelven salvo en el momento del alta.
 */
@Service
public class MfaService {

    private final MfaCredentialRepository credentials;
    private final MfaRecoveryCodeRepository recoveryCodes;
    private final UserAccountRepository users;
    private final TotpService totp;
    private final AuthProperties properties;

    public MfaService(
            MfaCredentialRepository credentials,
            MfaRecoveryCodeRepository recoveryCodes,
            UserAccountRepository users,
            TotpService totp,
            AuthProperties properties) {
        this.credentials = credentials;
        this.recoveryCodes = recoveryCodes;
        this.users = users;
        this.totp = totp;
        this.properties = properties;
    }

    /** Inicia el alta: genera secreto y URI otpauth. Aún inactivo hasta confirmar. */
    @Transactional
    public MfaEnrollment enroll(UUID userId) {
        credentials.findByUserId(userId).filter(MfaCredential::enabled).ifPresent(c -> {
            throw new ConflictException("mfa_already_enabled", "El segundo factor ya está activo");
        });
        var account = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user_not_found", "Usuario no encontrado"));
        String secret = totp.generateSecret();
        credentials.save(MfaCredential.startEnrollment(userId, secret));
        return new MfaEnrollment(secret, totp.otpAuthUri(secret, account.email()));
    }

    /** Confirma el alta verificando el primer código y devuelve los códigos de recuperación (una vez). */
    @Transactional
    public List<String> confirm(UUID userId, String code) {
        var credential = credentials
                .findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("mfa_not_enrolled", "No hay un enrolamiento en curso"));
        if (credential.enabled()) {
            throw new ConflictException("mfa_already_enabled", "El segundo factor ya está activo");
        }
        if (!totp.isValidCode(credential.secret(), code)) {
            throw new BusinessRuleException("invalid_mfa_code", "Código incorrecto");
        }
        credential.confirm();
        credentials.save(credential);

        List<String> codes = totp.generateRecoveryCodes(properties.mfa().recoveryCodes());
        recoveryCodes.replaceAll(
                userId, codes.stream().map(SecureTokens::sha256Hex).toList());
        return codes;
    }

    /** Desactiva el segundo factor (requiere un código válido) y elimina credencial y códigos. */
    @Transactional
    public void disable(UUID userId, String code) {
        var credential = credentials
                .findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException("mfa_not_enabled", "El segundo factor no está activo"));
        credential.requireEnabled();
        if (!verifyInternal(credential, userId, code)) {
            throw new BusinessRuleException("invalid_mfa_code", "Código incorrecto");
        }
        recoveryCodes.deleteByUserId(userId);
        credentials.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId) {
        return credentials.findByUserId(userId).map(MfaCredential::enabled).orElse(false);
    }

    /** Verifica un código (TOTP o de recuperación) para una cuenta con MFA activo. */
    @Transactional
    public boolean verify(UUID userId, String code) {
        return credentials
                .findByUserId(userId)
                .filter(MfaCredential::enabled)
                .map(credential -> verifyInternal(credential, userId, code))
                .orElse(false);
    }

    private boolean verifyInternal(MfaCredential credential, UUID userId, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        if (totp.isValidCode(credential.secret(), code)) {
            return true;
        }
        return recoveryCodes.consume(userId, SecureTokens.sha256Hex(code.trim()));
    }
}
