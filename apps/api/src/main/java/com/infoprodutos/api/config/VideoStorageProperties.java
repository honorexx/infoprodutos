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
        if (maxFileBytes <= 0) {
            maxFileBytes = 500L * 1024 * 1024;
        }
        if (streamUrlTtlSeconds <= 0) {
            streamUrlTtlSeconds = 300;
        }
    }
}
