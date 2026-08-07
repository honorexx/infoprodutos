CREATE TABLE ai_generation_job (
    id                         UUID PRIMARY KEY,
    course_id                  UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    module_id                  UUID NOT NULL REFERENCES module (id) ON DELETE CASCADE,
    lesson_id                  UUID NOT NULL REFERENCES lesson (id) ON DELETE CASCADE,
    video_asset_id             UUID REFERENCES video_asset (id) ON DELETE SET NULL,
    transcript_id              UUID REFERENCES transcript (id) ON DELETE SET NULL,
    status                     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider                   VARCHAR(100),
    model                      VARCHAR(100),
    requested_question_count   INT NOT NULL,
    difficulty_distribution    JSONB,
    language                   VARCHAR(10) NOT NULL,
    extra_instructions         TEXT,
    idempotency_key            VARCHAR(100) NOT NULL UNIQUE,
    attempt_count              INT NOT NULL DEFAULT 0,
    error_message              VARCHAR(500),
    usage_metadata             JSONB,
    requested_by_user_id       UUID NOT NULL REFERENCES app_user (id),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at                 TIMESTAMPTZ,
    completed_at               TIMESTAMPTZ,

    CONSTRAINT chk_ai_job_status CHECK (status IN (
        'PENDING', 'TRANSCRIBING', 'TRANSCRIBED', 'GENERATING',
        'AWAITING_REVIEW', 'COMPLETED', 'FAILED', 'CANCELLED'
    ))
);

CREATE INDEX idx_ai_job_lesson ON ai_generation_job (lesson_id, created_at DESC);
CREATE INDEX idx_ai_job_status ON ai_generation_job (status);
CREATE INDEX idx_ai_job_requested_by ON ai_generation_job (requested_by_user_id);

ALTER TABLE question
    ADD CONSTRAINT fk_question_ai_job
    FOREIGN KEY (ai_generation_job_id) REFERENCES ai_generation_job (id) ON DELETE SET NULL;

CREATE TABLE ai_generated_question_review (
    id                     UUID PRIMARY KEY,
    ai_generation_job_id   UUID NOT NULL REFERENCES ai_generation_job (id) ON DELETE CASCADE,
    question_id            UUID NOT NULL UNIQUE REFERENCES question (id) ON DELETE CASCADE,
    raw_ai_payload         JSONB NOT NULL,
    reviewed_by_user_id    UUID REFERENCES app_user (id) ON DELETE SET NULL,
    review_status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    review_notes           TEXT,
    reviewed_at            TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_ai_review_status CHECK (review_status IN ('PENDING', 'APPROVED', 'REJECTED', 'REGENERATED'))
);

CREATE INDEX idx_ai_review_job ON ai_generated_question_review (ai_generation_job_id);
