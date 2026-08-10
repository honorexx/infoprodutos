-- Thumbnail obrigatória em novos uploads de vídeo (nullable para assets legados).
ALTER TABLE video_asset
    ADD COLUMN IF NOT EXISTS thumbnail_storage_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS thumbnail_mime_type VARCHAR(100);

COMMENT ON COLUMN video_asset.thumbnail_storage_key IS
    'Capa/poster do vídeo. Obrigatória em novos uploads; assets antigos podem ser NULL.';
