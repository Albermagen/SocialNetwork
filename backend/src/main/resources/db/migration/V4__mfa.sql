-- Fase 1 (auth): segundo factor TOTP opcional. Una credencial por usuario (secreto compartido) y
-- un conjunto de códigos de recuperación de un solo uso (solo se guarda su hash).
-- Mejora futura (fase 10): cifrar `secret` en reposo con una clave gestionada (KMS).
CREATE TABLE mfa_credentials (
    user_id      uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    secret       text        NOT NULL,
    enabled      boolean     NOT NULL DEFAULT false,
    created_at   timestamptz NOT NULL DEFAULT now(),
    confirmed_at timestamptz
);

CREATE TABLE mfa_recovery_codes (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  text        NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_mfa_recovery_hash UNIQUE (code_hash)
);

CREATE INDEX idx_mfa_recovery_user ON mfa_recovery_codes (user_id);
