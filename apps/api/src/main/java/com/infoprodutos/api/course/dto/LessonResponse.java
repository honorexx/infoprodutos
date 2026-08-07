package com.infoprodutos.api.course.dto;

import com.infoprodutos.api.course.domain.Lesson;

public record LessonResponse(
        String id,
        String moduleId,
        String title,
        String description,
        int orderIndex,
        Integer durationSeconds,
        String accessType,
        String status,
        String currentVideoAssetId) {

    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId().toString(),
                lesson.getModule().getId().toString(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getOrderIndex(),
                lesson.getDurationSeconds(),
                lesson.getAccessType().name(),
                lesson.getStatus().name(),
                lesson.getCurrentVideoAssetId() != null ? lesson.getCurrentVideoAssetId().toString() : null);
    }
}
