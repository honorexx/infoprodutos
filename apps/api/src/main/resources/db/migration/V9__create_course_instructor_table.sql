CREATE TABLE course_instructor (
    id                  UUID PRIMARY KEY,
    course_id           UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    instructor_user_id  UUID NOT NULL REFERENCES app_user (id),
    is_primary          BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_course_instructor UNIQUE (course_id, instructor_user_id)
);

CREATE INDEX idx_course_instructor_instructor ON course_instructor (instructor_user_id);

COMMENT ON TABLE course_instructor IS 'Vínculo N:N entre curso e professor(es) responsáveis - define posse para autorização.';
