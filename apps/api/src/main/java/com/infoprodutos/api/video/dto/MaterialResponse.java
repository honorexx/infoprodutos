package com.infoprodutos.api.video.dto;

import com.infoprodutos.api.video.domain.LessonMaterial;

public record MaterialResponse(
        String id,
        String lessonId,
        String title,
        String mimeType,
        Long sizeBytes,
        int orderIndex,
        String createdAt) {

    public static MaterialResponse from(LessonMaterial material) {
        return new MaterialResponse(
                material.getId().toString(),
                material.getLessonId().toString(),
                material.getTitle(),
                material.getMimeType(),
                material.getSizeBytes(),
                material.getOrderIndex(),
                material.getCreatedAt().toString());
    }
}
