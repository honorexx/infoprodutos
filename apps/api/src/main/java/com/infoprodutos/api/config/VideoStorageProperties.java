package com.infoprodutos.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.video-storage")
public record VideoStorageProperties(
        String localRoot,
        long maxFileBytes,
        int streamUrlTtlSeconds) {

    public VideoStorageProperties {
        if (localRoot == null || localRoot.isBlank()) {
            localRoot = "./data/videos";
        }
        // Default 2 GiB — aulas de 20+ min em MP4 razoável.
        if (maxFileBytes <= 0) {
            maxFileBytes = 2L * 1024 * 1024 * 1024;
        }
        if (streamUrlTtlSeconds <= 0) {
            streamUrlTtlSeconds = 300;
        }
    }
}
