CREATE TABLE password_reset_token (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token (user_id);
