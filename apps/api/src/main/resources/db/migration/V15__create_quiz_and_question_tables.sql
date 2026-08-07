CREATE TABLE quiz (
    id              UUID PRIMARY KEY,
    module_id       UUID NOT NULL UNIQUE REFERENCES module (id) ON DELETE CASCADE,
    title           VARCHAR(200),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    passing_score   NUMERIC(5, 2),
    max_attempts    INT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT chk_quiz_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE TABLE question (
    id                      UUID PRIMARY KEY,
    quiz_id                 UUID NOT NULL REFERENCES quiz (id) ON DELETE CASCADE,
    lesson_id               UUID NOT NULL REFERENCES lesson (id) ON DELETE CASCADE,
    transcript_segment_id   UUID REFERENCES transcript_segment (id) ON DELETE SET NULL,
    statement               TEXT NOT NULL,
    explanation             TEXT,
    difficulty              VARCHAR(10) NOT NULL,
    topic                   VARCHAR(255),
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    origin                  VARCHAR(20) NOT NULL,
    ai_generation_job_id    UUID,
    order_index             INT NOT NULL DEFAULT 0,
    approved_by_user_id     UUID REFERENCES app_user (id) ON DELETE SET NULL,
    approved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at              TIMESTAMPTZ,

    CONSTRAINT chk_question_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_question_status CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED', 'PUBLISHED')),
    CONSTRAINT chk_question_origin CHECK (origin IN ('MANUAL', 'AI_GENERATED'))
);

CREATE INDEX idx_question_quiz ON question (quiz_id, order_index);
CREATE INDEX idx_question_lesson ON question (lesson_id);
CREATE INDEX idx_question_job ON question (ai_generation_job_id);

CREATE TABLE question_option (
    id           UUID PRIMARY KEY,
    question_id  UUID NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    text         VARCHAR(500) NOT NULL,
    is_correct   BOOLEAN NOT NULL DEFAULT false,
    order_index  INT NOT NULL
);

CREATE INDEX idx_question_option_question ON question_option (question_id, order_index);

CREATE UNIQUE INDEX uq_question_option_correct
    ON question_option (question_id)
    WHERE is_correct = true;
