package com.infoprodutos.api.video.dto;

public record StreamUrlResponse(
        String url,
        String thumbnailUrl,
        long expiresAtEpochSeconds,
        int ttlSeconds) {}
