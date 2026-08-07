package com.infoprodutos.api.video.dto;

public record UploadInitResponse(
        String videoAssetId,
        String uploadUrl,
        String uploadStatus) {}
