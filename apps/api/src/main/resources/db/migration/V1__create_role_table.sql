CREATE TABLE role (
    id          UUID PRIMARY KEY,
    code        VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_role_code UNIQUE (code)
);

COMMENT ON TABLE role IS 'Papéis da plataforma (tabela de referência, não enum de banco - ver docs/DATABASE.md secao 2).';
