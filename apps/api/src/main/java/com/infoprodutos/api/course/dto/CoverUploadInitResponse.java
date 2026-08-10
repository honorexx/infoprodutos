package com.infoprodutos.api.course.dto;

public record CoverUploadInitResponse(
        String uploadMode,
        String uploadUrl,
        String storageKey,
        String contentType) {}
