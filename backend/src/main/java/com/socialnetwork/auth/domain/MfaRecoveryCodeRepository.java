package com.socialnetwork.auth.domain;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de persistencia de códigos de recuperación MFA. Solo se almacenan hashes. Adaptador JPA en
 * {@code infrastructure.persistence}.
 */
public interface MfaRecoveryCodeRepository {

    /** Reemplaza el conjunto de códigos del usuario por los hashes dados. */
    void replaceAll(UUID userId, List<String> codeHashes);

    /** Consume un código sin usar que coincida con el hash; devuelve true si lo encontró y lo marcó. */
    boolean consume(UUID userId, String codeHash);

    void deleteByUserId(UUID userId);
}
