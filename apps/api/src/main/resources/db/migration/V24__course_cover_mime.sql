-- MIME da capa local (cover_image_url guarda a storage_key quando não for URL http).
ALTER TABLE course
    ADD COLUMN IF NOT EXISTS cover_mime_type VARCHAR(100);

COMMENT ON COLUMN course.cover_image_url IS
    'URL http(s) externa OU storage_key local do arquivo de capa.';
COMMENT ON COLUMN course.cover_mime_type IS
    'MIME da capa quando armazenada localmente (image/jpeg, image/png, image/webp).';
