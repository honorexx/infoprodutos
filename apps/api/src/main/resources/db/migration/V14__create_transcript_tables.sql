CREATE TABLE transcript (
    id              UUID PRIMARY KEY,
    video_asset_id  UUID NOT NULL UNIQUE REFERENCES video_asset (id) ON DELETE CASCADE,
    language        VARCHAR(10) NOT NULL,
    full_text       TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider        VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,

    CONSTRAINT chk_transcript_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE transcript_segment (
    id                   UUID PRIMARY KEY,
    transcript_id        UUID NOT NULL REFERENCES transcript (id) ON DELETE CASCADE,
    sequence_index       INT NOT NULL,
    start_time_seconds   NUMERIC(10, 2) NOT NULL,
    end_time_seconds     NUMERIC(10, 2) NOT NULL,
    text                 TEXT NOT NULL,
    topic                VARCHAR(255),

    CONSTRAINT uq_transcript_segment UNIQUE (transcript_id, sequence_index)
);

CREATE INDEX idx_transcript_segment_transcript ON transcript_segment (transcript_id);
