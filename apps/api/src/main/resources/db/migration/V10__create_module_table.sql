CREATE TABLE module (
    id           UUID PRIMARY KEY,
    course_id    UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    order_index  INT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT chk_module_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

-- Não é UNIQUE a nível de banco: reordenação em lote reatribui order_index
-- dentro de uma transação na camada de serviço (ver docs/DATABASE.md §5.5).
CREATE INDEX idx_module_course_order ON module (course_id, order_index);

COMMENT ON TABLE module IS 'Módulo de um curso; ordem e publicação controladas pelo serviço, não apenas pelo banco.';
