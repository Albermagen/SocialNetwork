-- Fase 1 (auth): cuentas de usuario y tokens de email (verificación / reset de contraseña).
-- Los refresh tokens NO se persisten aquí: viven en Redis con TTL, rotación y detección de
-- reuso por familia (ver RedisRefreshTokenStore). Ver docs/DATABASE.md §1.

CREATE TABLE users (
    id             uuid PRIMARY KEY,
    username       citext      NOT NULL,
    email          citext      NOT NULL,
    password_hash  text,       -- NULL cuando la cuenta sea solo OAuth (iteración OAuth2)
    email_verified boolean     NOT NULL DEFAULT false,
    role           varchar(20) NOT NULL DEFAULT 'USER',
    status         varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_username_format CHECK (username ~ '^[A-Za-z0-9_]{3,30}$'),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'MODERATOR', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE TABLE email_tokens (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash text        NOT NULL,
    type       varchar(10) NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_email_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_email_tokens_type CHECK (type IN ('VERIFY', 'RESET'))
);

CREATE INDEX idx_email_tokens_user_id ON email_tokens (user_id);
