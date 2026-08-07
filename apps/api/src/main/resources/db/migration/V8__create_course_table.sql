CREATE TABLE course (
    id                         UUID PRIMARY KEY,
    title                      VARCHAR(200) NOT NULL,
    slug                       VARCHAR(220) NOT NULL,
    description                TEXT,
    cover_image_url            VARCHAR(500),
    workload_hours             NUMERIC(6, 2),
    status                     VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    min_completion_percentage  NUMERIC(5, 2) NOT NULL DEFAULT 100,
    min_passing_score          NUMERIC(5, 2) NOT NULL DEFAULT 70,
    certificate_enabled        BOOLEAN NOT NULL DEFAULT true,
    max_quiz_attempts          INT,
    created_by_user_id         UUID NOT NULL REFERENCES app_user (id),
    published_at               TIMESTAMPTZ,
    archived_at                TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at                 TIMESTAMPTZ,

    CONSTRAINT chk_course_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uq_course_slug ON course (slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_course_status ON course (status);
CREATE INDEX idx_course_created_by ON course (created_by_user_id);

COMMENT ON TABLE course IS 'Curso (fase 2 - sem vídeo/matrícula ainda). "Despublicar" volta PUBLISHED -> DRAFT (ver docs/DECISIONS.md).';
COMMENT ON COLUMN course.slug IS 'Identificador amigável único usado em URLs públicas futuras.';
