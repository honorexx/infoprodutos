-- Certificados (Fase 8) + conclusão formal da matrícula
ALTER TABLE enrollment
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;

COMMENT ON COLUMN enrollment.completed_at IS 'Momento em que o aluno concluiu formalmente o curso (todas as aulas publicadas).';

CREATE TABLE certificate (
    id                                UUID PRIMARY KEY,
    enrollment_id                     UUID NOT NULL UNIQUE REFERENCES enrollment (id),
    student_user_id                   UUID NOT NULL REFERENCES app_user (id),
    course_id                         UUID NOT NULL REFERENCES course (id),
    validation_code                   VARCHAR(32) NOT NULL UNIQUE,
    status                            VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    student_name_snapshot             VARCHAR(150) NOT NULL,
    course_title_snapshot             VARCHAR(200) NOT NULL,
    workload_hours_snapshot           NUMERIC(6, 2) NOT NULL,
    completion_date                   DATE NOT NULL,
    issued_at                         TIMESTAMPTZ NOT NULL DEFAULT now(),
    coordinator_name_snapshot         VARCHAR(150) NOT NULL,
    chief_vision_officer_name_snapshot VARCHAR(150) NOT NULL,
    pdf_path                          VARCHAR(500),
    validation_url                    VARCHAR(500) NOT NULL,
    revoked_at                        TIMESTAMPTZ,
    created_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_certificate_status CHECK (status IN ('ISSUED', 'REVOKED'))
);

CREATE INDEX idx_certificate_student ON certificate (student_user_id);
CREATE INDEX idx_certificate_validation_code ON certificate (validation_code);

COMMENT ON TABLE certificate IS 'Certificado institucional da plataforma (snapshots imutáveis). Sem dependência do professor do curso.';
