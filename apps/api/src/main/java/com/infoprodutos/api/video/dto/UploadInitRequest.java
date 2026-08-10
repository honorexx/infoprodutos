package com.infoprodutos.api.video.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

/**
 * Inicia upload de vídeo. Com S3/R2, envie metadados do arquivo para gerar URLs assinadas.
 * Campos de arquivo são opcionais no modo PROXY (storage local) por compatibilidade.
 */
public record UploadInitRequest(
        @NotNull UUID lessonId,
        String videoContentType,
        String videoFilename,
        @PositiveOrZero Long videoSizeBytes,
        String thumbnailContentType,
        String thumbnailFilename,
        @PositiveOrZero Long thumbnailSizeBytes) {}
