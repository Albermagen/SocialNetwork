-- Fase 1 (auth): identidades externas (OAuth2). Una cuenta de usuario puede tener varias
-- identidades (Google hoy; más proveedores en el futuro). El login social localiza al usuario por
-- (provider, provider_user_id); si no existe y el email viene verificado por el proveedor, se
-- vincula a la cuenta local con ese email; si no, se crea una cuenta nueva sin contraseña.
CREATE TABLE oauth_identities (
    id               uuid PRIMARY KEY,
    user_id          uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         varchar(20)  NOT NULL,
    provider_user_id varchar(255) NOT NULL,
    email            citext,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_oauth_provider CHECK (provider IN ('GOOGLE'))
);

CREATE INDEX idx_oauth_identities_user_id ON oauth_identities (user_id);
