CREATE TABLE lesson_material (
    id                UUID PRIMARY KEY,
    lesson_id         UUID NOT NULL REFERENCES lesson (id) ON DELETE CASCADE,
    title             VARCHAR(200) NOT NULL,
    storage_provider  VARCHAR(30) NOT NULL,
    storage_key       VARCHAR(500) NOT NULL,
    mime_type         VARCHAR(100),
    size_bytes        BIGINT,
    order_index       INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT chk_material_storage_provider CHECK (storage_provider IN ('LOCAL_DEV', 'S3_COMPATIBLE', 'EXTERNAL_STREAMING'))
);

CREATE INDEX idx_lesson_material_lesson ON lesson_material (lesson_id, order_index);
