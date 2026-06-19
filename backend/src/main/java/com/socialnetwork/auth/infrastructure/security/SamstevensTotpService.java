package com.socialnetwork.auth.infrastructure.security;

import com.socialnetwork.auth.application.AuthProperties;
import com.socialnetwork.auth.application.port.TotpService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/** Adaptador TOTP sobre la librería {@code dev.samstevens.totp}. */
@Component
class SamstevensTotpService implements TotpService {

    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;
    private static final HashingAlgorithm ALGORITHM = HashingAlgorithm.SHA1;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final RecoveryCodeGenerator recoveryCodeGenerator = new RecoveryCodeGenerator();
    private final CodeVerifier codeVerifier;
    private final AuthProperties properties;

    SamstevensTotpService(AuthProperties properties) {
        this.properties = properties;
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        // Tolera ±1 ventana (±30 s) por desfase de reloj entre servidor y dispositivo.
        verifier.setAllowedTimePeriodDiscrepancy(1);
        this.codeVerifier = verifier;
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public String otpAuthUri(String secret, String accountName) {
        return new QrData.Builder()
                .label(accountName)
                .secret(secret)
                .issuer(properties.mfa().issuer())
                .algorithm(ALGORITHM)
                .digits(DIGITS)
                .period(PERIOD_SECONDS)
                .build()
                .getUri();
    }

    @Override
    public boolean isValidCode(String secret, String code) {
        return code != null && codeVerifier.isValidCode(secret, code.trim());
    }

    @Override
    public List<String> generateRecoveryCodes(int count) {
        return Arrays.asList(recoveryCodeGenerator.generateCodes(count));
    }
}
