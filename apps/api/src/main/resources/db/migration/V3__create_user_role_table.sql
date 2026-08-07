CREATE TABLE user_role (
    user_id    UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES role (id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_role_role_id ON user_role (role_id);

COMMENT ON TABLE user_role IS 'Relação N:N entre usuário e papel (um usuário pode acumular mais de um papel - docs/DECISIONS.md).';
