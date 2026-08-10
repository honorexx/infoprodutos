package com.infoprodutos.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Object storage S3-compatible (Cloudflare R2, AWS S3, MinIO).
 * Quando {@code enabled=true}, uploads de vídeo usam URL assinada (PUT direto do browser).
 */
@ConfigurationProperties(prefix = "app.video-storage.s3")
public record S3StorageProperties(
        boolean enabled,
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        /** Opcional: CDN/público. Playback usa presigned GET se vazio. */
        String publicBaseUrl,
        int uploadUrlTtlSeconds,
        int downloadUrlTtlSeconds) {

    public S3StorageProperties {
        if (region == null || region.isBlank()) {
            region = "auto";
        }
        if (uploadUrlTtlSeconds <= 0) {
            uploadUrlTtlSeconds = 3600;
        }
        if (downloadUrlTtlSeconds <= 0) {
            downloadUrlTtlSeconds = 300;
        }
        if (accessKey == null) {
            accessKey = "";
        }
        if (secretKey == null) {
            secretKey = "";
        }
        if (endpoint == null) {
            endpoint = "";
        }
        if (bucket == null) {
            bucket = "";
        }
        if (publicBaseUrl == null) {
            publicBaseUrl = "";
        }
    }

    public boolean isConfigured() {
        return enabled
                && !endpoint.isBlank()
                && !bucket.isBlank()
                && !accessKey.isBlank()
                && !secretKey.isBlank();
    }
}
