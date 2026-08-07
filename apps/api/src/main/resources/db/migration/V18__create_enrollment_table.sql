CREATE TABLE enrollment (
    id                   UUID PRIMARY KEY,
    student_user_id      UUID NOT NULL REFERENCES app_user (id),
    course_id            UUID NOT NULL REFERENCES course (id),
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at           TIMESTAMPTZ,
    granted_by_user_id   UUID REFERENCES app_user (id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT uq_enrollment_student_course UNIQUE (student_user_id, course_id)
);

CREATE INDEX idx_enrollment_student_status ON enrollment (student_user_id, status);
CREATE INDEX idx_enrollment_course_status ON enrollment (course_id, status);

COMMENT ON TABLE enrollment IS 'Matrícula manual aluno/curso (Fase 4). Uma linha por par; status muda sem nova linha.';
