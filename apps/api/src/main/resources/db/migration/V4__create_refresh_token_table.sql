-- Tabela adicional em relação à lista original de docs/DATABASE.md,
-- necessária para permitir revogação/rotação segura de sessões
-- (logout efetivo, bloqueio imediato de conta) - ver docs/DECISIONS.md.
CREATE TABLE refresh_token (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash          VARCHAR(255) NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    replaced_by_token_id UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);

COMMENT ON TABLE refresh_token IS 'Apenas o hash do token é armazenado, nunca o valor bruto (docs/SECURITY.md secao 2).';
