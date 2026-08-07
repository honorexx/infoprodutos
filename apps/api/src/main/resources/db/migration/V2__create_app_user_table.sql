-- Nome da tabela é "app_user" (em vez de "user") porque USER é palavra
-- reservada no padrão SQL/PostgreSQL - ver docs/DECISIONS.md.
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,

    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE UNIQUE INDEX uq_app_user_email ON app_user (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_app_user_status ON app_user (status);

COMMENT ON TABLE app_user IS 'Usuários da plataforma (SUPER_ADMIN, INSTRUCTOR, STUDENT via user_role).';
COMMENT ON COLUMN app_user.status IS 'Bloqueio lógico (BLOCKED) nunca remove o histórico do usuário.';
