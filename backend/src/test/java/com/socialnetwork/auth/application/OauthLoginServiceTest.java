package com.socialnetwork.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.socialnetwork.auth.domain.OauthIdentity;
import com.socialnetwork.auth.domain.OauthIdentityRepository;
import com.socialnetwork.auth.domain.OauthProvider;
import com.socialnetwork.auth.domain.UserAccount;
import com.socialnetwork.auth.domain.UserAccountRepository;
import com.socialnetwork.shared.error.BusinessRuleException;
import com.socialnetwork.shared.util.Uuids;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lógica de resolución de cuenta a partir de una identidad externa (sin contexto de Spring). */
class OauthLoginServiceTest {

    private FakeIdentityRepo identities;
    private FakeUserRepo users;
    private OauthLoginService service;

    @BeforeEach
    void setUp() {
        identities = new FakeIdentityRepo();
        users = new FakeUserRepo();
        service = new OauthLoginService(identities, users);
    }

    @Test
    void existingLinkReturnsItsUser() {
        UUID userId = Uuids.v7();
        identities.save(OauthIdentity.link(Uuids.v7(), userId, OauthProvider.GOOGLE, "sub-1", "a@example.com"));

        UUID resolved = service.resolveAccount(OauthProvider.GOOGLE, "sub-1", "a@example.com", true, "Alberto");

        assertThat(resolved).isEqualTo(userId);
    }

    @Test
    void linksToExistingLocalAccountWhenEmailVerified() {
        UserAccount local = UserAccount.register(Uuids.v7(), "alberto", "a@example.com", "$2a$12$hash");
        users.save(local);

        UUID resolved = service.resolveAccount(OauthProvider.GOOGLE, "sub-2", "a@example.com", true, "Alberto");

        assertThat(resolved).isEqualTo(local.id());
        assertThat(identities.findByProviderAndProviderUserId(OauthProvider.GOOGLE, "sub-2"))
                .isPresent();
        assertThat(users.store).hasSize(1); // no se creó cuenta nueva
    }

    @Test
    void refusesLinkingWhenProviderEmailNotVerified() {
        users.save(UserAccount.register(Uuids.v7(), "alberto", "a@example.com", "$2a$12$hash"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.resolveAccount(OauthProvider.GOOGLE, "sub-3", "a@example.com", false, "A"))
                .matches(ex -> ex.code().equals("oauth_email_unverified"));
    }

    @Test
    void createsNewAccountWhenNoneExists() {
        UUID resolved =
                service.resolveAccount(OauthProvider.GOOGLE, "sub-4", "nuevo@example.com", true, "Nuevo Usuario");

        UserAccount created = users.findById(resolved).orElseThrow();
        assertThat(created.passwordHash()).isNull(); // cuenta solo-OAuth
        assertThat(created.emailVerified()).isTrue();
        assertThat(created.username()).matches("^[A-Za-z0-9_]{3,30}$");
    }

    @Test
    void generatesUniqueUsernameOnCollision() {
        users.save(UserAccount.register(Uuids.v7(), "nuevo", "otro@example.com", "$2a$12$hash"));

        UUID resolved = service.resolveAccount(OauthProvider.GOOGLE, "sub-5", "nuevo@example.com", true, "nuevo");

        assertThat(users.findById(resolved).orElseThrow().username()).isNotEqualTo("nuevo");
    }

    @Test
    void missingEmailIsRejected() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service.resolveAccount(OauthProvider.GOOGLE, "sub-6", null, true, "X"))
                .matches(ex -> ex.code().equals("oauth_email_missing"));
    }

    // --- Fakes en memoria ---

    private static final class FakeIdentityRepo implements OauthIdentityRepository {
        private final Map<String, OauthIdentity> store = new HashMap<>();

        @Override
        public Optional<OauthIdentity> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId) {
            return Optional.ofNullable(store.get(provider + ":" + providerUserId));
        }

        @Override
        public OauthIdentity save(OauthIdentity identity) {
            store.put(identity.provider() + ":" + identity.providerUserId(), identity);
            return identity;
        }
    }

    private static final class FakeUserRepo implements UserAccountRepository {
        private final Map<UUID, UserAccount> store = new HashMap<>();

        @Override
        public Optional<UserAccount> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<UserAccount> findByIdentifier(String identifier) {
            return store.values().stream()
                    .filter(u -> u.username().equalsIgnoreCase(identifier)
                            || u.email().equalsIgnoreCase(identifier))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return store.values().stream()
                    .filter(u -> u.email().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return store.values().stream().anyMatch(u -> u.username().equalsIgnoreCase(username));
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public UserAccount save(UserAccount account) {
            store.put(account.id(), account);
            return account;
        }
    }
}
