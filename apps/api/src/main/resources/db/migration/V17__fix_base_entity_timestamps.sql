-- BaseEntity exige created_at; estas tabelas ficaram sem a coluna na V14/V15.
ALTER TABLE question_option
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE transcript_segment
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
