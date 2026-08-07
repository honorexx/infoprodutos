CREATE TABLE quiz_attempt (
    id               UUID PRIMARY KEY,
    enrollment_id    UUID NOT NULL REFERENCES enrollment (id),
    quiz_id          UUID NOT NULL REFERENCES quiz (id),
    attempt_number   INT NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at     TIMESTAMPTZ,
    score            NUMERIC(5, 2),
    passed           BOOLEAN,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_quiz_attempt_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED')),
    CONSTRAINT chk_quiz_attempt_number CHECK (attempt_number >= 1),
    CONSTRAINT uq_quiz_attempt_enrollment_quiz_number UNIQUE (enrollment_id, quiz_id, attempt_number)
);

CREATE INDEX idx_quiz_attempt_enrollment ON quiz_attempt (enrollment_id);
CREATE INDEX idx_quiz_attempt_quiz ON quiz_attempt (quiz_id);

CREATE TABLE student_answer (
    id                   UUID PRIMARY KEY,
    quiz_attempt_id      UUID NOT NULL REFERENCES quiz_attempt (id),
    question_id          UUID NOT NULL REFERENCES question (id),
    selected_option_id   UUID NOT NULL REFERENCES question_option (id),
    is_correct           BOOLEAN NOT NULL,
    answered_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_student_answer_attempt_question UNIQUE (quiz_attempt_id, question_id)
);

CREATE INDEX idx_student_answer_attempt ON student_answer (quiz_attempt_id);

COMMENT ON TABLE quiz_attempt IS 'Tentativa de quiz do aluno (Fase 5). Imutável após SUBMITTED/GRADED.';
COMMENT ON TABLE student_answer IS 'Resposta a uma questão dentro de uma tentativa. Correção determinística.';
