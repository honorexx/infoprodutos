CREATE TABLE lesson_progress (
    id                     UUID PRIMARY KEY,
    enrollment_id          UUID NOT NULL REFERENCES enrollment (id),
    lesson_id              UUID NOT NULL REFERENCES lesson (id),
    status                 VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    last_position_seconds  INT NOT NULL DEFAULT 0,
    started_at             TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_lesson_progress_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_lesson_progress_position CHECK (last_position_seconds >= 0),
    CONSTRAINT uq_lesson_progress_enrollment_lesson UNIQUE (enrollment_id, lesson_id)
);

CREATE INDEX idx_lesson_progress_enrollment_status ON lesson_progress (enrollment_id, status);

COMMENT ON TABLE lesson_progress IS 'Progresso por aula na matrícula. Conclusão: >=90% do vídeo ou marcação manual (monotônico).';
