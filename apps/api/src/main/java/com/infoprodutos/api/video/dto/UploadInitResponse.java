package com.infoprodutos.api.video.dto;

/**
 * @param uploadMode {@code DIRECT} = PUT no object storage; {@code PROXY} = multipart na API
 */
public record UploadInitResponse(
        String videoAssetId,
        String uploadMode,
        String uploadUrl,
        String videoUploadUrl,
        String thumbnailUploadUrl,
        String videoContentType,
        String thumbnailContentType,
        String uploadStatus) {}
