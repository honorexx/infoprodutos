CREATE TABLE video_asset (
    id                  UUID PRIMARY KEY,
    lesson_id           UUID REFERENCES lesson (id) ON DELETE SET NULL,
    storage_provider    VARCHAR(30) NOT NULL,
    storage_key         VARCHAR(500) NOT NULL,
    original_filename   VARCHAR(255),
    mime_type           VARCHAR(100),
    size_bytes          BIGINT,
    duration_seconds    INT,
    upload_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(500),
    checksum            VARCHAR(128),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_video_storage_provider CHECK (storage_provider IN ('LOCAL_DEV', 'S3_COMPATIBLE', 'EXTERNAL_STREAMING')),
    CONSTRAINT chk_video_upload_status CHECK (upload_status IN ('PENDING', 'UPLOADING', 'UPLOADED', 'FAILED')),
    CONSTRAINT chk_video_processing_status CHECK (processing_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED'))
);

CREATE INDEX idx_video_asset_lesson ON video_asset (lesson_id);

ALTER TABLE lesson
    ADD COLUMN current_video_asset_id UUID REFERENCES video_asset (id) ON DELETE SET NULL;

COMMENT ON TABLE video_asset IS 'Histórico de vídeos por aula. Substituir cria novo registro; o antigo permanece.';
