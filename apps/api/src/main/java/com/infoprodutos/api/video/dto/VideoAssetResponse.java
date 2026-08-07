package com.infoprodutos.api.video.dto;

import com.infoprodutos.api.video.domain.VideoAsset;

public record VideoAssetResponse(
        String id,
        String lessonId,
        String originalFilename,
        String mimeType,
        Long sizeBytes,
        Integer durationSeconds,
        String uploadStatus,
        String processingStatus,
        String failureReason,
        String createdAt) {

    public static VideoAssetResponse from(VideoAsset asset) {
        return new VideoAssetResponse(
                asset.getId().toString(),
                asset.getLessonId() != null ? asset.getLessonId().toString() : null,
                asset.getOriginalFilename(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getDurationSeconds(),
                asset.getUploadStatus().name(),
                asset.getProcessingStatus().name(),
                asset.getFailureReason(),
                asset.getCreatedAt().toString());
    }
}
