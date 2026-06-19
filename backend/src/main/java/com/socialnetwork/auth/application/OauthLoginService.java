package com.socialnetwork.auth.application;

import com.socialnetwork.auth.domain.OauthIdentity;
import com.socialnetwork.auth.domain.OauthIdentityRepository;
import com.socialnetwork.auth.domain.OauthProvider;
import com.socialnetwork.auth.domain.UserAccount;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.BusinessRuleException;
import com.socialnetwork.shared.util.Uuids;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: resolución de la cuenta local a partir de una identidad externa (login social).
 *
 * <p>Reglas: (1) si ya existe el vínculo {@code (provider, providerUserId)} se devuelve su usuario;
 * (2) si no, y hay una cuenta local con ese email <em>verificado por el proveedor</em>, se vincula
 * a ella (account linking); (3) en otro caso se crea una cuenta nueva sin contraseña. Vincular solo
 * con email verificado evita el secuestro de cuentas por un email no probado.
 */
@Service
public class OauthLoginService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OauthIdentityRepository identities;
    private final UserAccountRepository users;

    public OauthLoginService(OauthIdentityRepository identities, UserAccountRepository users) {
        this.identities = identities;
        this.users = users;
    }

    /** Devuelve el id de la cuenta local asociada a la identidad externa, creándola/vinculándola si hace falta. */
    @Transactional
    public UUID resolveAccount(
            OauthProvider provider, String providerUserId, String email, boolean emailVerified, String displayName) {
        var existingLink = identities.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingLink.isPresent()) {
            return existingLink.get().userId();
        }
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("oauth_email_missing", "El proveedor no facilitó un email");
        }

        UserAccount account = users.findByEmail(email)
                .map(local -> linkToExistingAccount(local, emailVerified))
                .orElseGet(() -> createAccount(email, emailVerified, displayName));

        identities.save(OauthIdentity.link(Uuids.v7(), account.id(), provider, providerUserId, email));
        return account.id();
    }

    private UserAccount linkToExistingAccount(UserAccount account, boolean emailVerified) {
        if (!emailVerified) {
            throw new BusinessRuleException(
                    "oauth_email_unverified", "El proveedor no ha verificado el email; no se vincula la cuenta");
        }
        return account;
    }

    private UserAccount createAccount(String email, boolean emailVerified, String displayName) {
        UserAccount account =
                UserAccount.registerOauth(Uuids.v7(), generateUsername(email, displayName), email, emailVerified);
        return users.save(account);
    }

    /** Genera un username único: base a partir del nombre o del email, saneada, con sufijo si colisiona. */
    private String generateUsername(String email, String displayName) {
        String base = sanitize(displayName);
        if (base.length() < 3) {
            base = sanitize(email.contains("@") ? email.substring(0, email.indexOf('@')) : email);
        }
        if (base.length() < 3) {
            base = "user";
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        String candidate = base;
        while (users.existsByUsername(candidate)) {
            candidate = base + "_" + (1000 + RANDOM.nextInt(9000));
        }
        return candidate;
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
    }
}
