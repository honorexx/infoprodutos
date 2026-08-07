CREATE TABLE lesson (
    id                UUID PRIMARY KEY,
    module_id         UUID NOT NULL REFERENCES module (id) ON DELETE CASCADE,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    order_index       INT NOT NULL,
    duration_seconds  INT,
    access_type       VARCHAR(20) NOT NULL DEFAULT 'ENROLLED_ONLY',
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT chk_lesson_access_type CHECK (access_type IN ('FREE_PREVIEW', 'ENROLLED_ONLY')),
    CONSTRAINT chk_lesson_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_lesson_module_order ON lesson (module_id, order_index);

COMMENT ON TABLE lesson IS 'Aula de um módulo. Sem vídeo nesta fase - a coluna current_video_asset_id (FK -> video_asset) será adicionada na Fase 3, junto com a tabela video_asset (ver docs/DECISIONS.md).';
