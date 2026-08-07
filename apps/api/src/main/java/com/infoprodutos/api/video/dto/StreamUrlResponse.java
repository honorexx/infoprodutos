package com.infoprodutos.api.video.dto;

public record StreamUrlResponse(String url, long expiresAtEpochSeconds, int ttlSeconds) {}
