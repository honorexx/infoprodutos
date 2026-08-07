package com.infoprodutos.api.enrollment.dto;

import com.infoprodutos.api.enrollment.domain.LessonProgress;
import java.time.Instant;

public record LessonProgressResponse(
        String id,
        String enrollmentId,
        String lessonId,
        String status,
        int lastPositionSeconds,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt) {

    public static LessonProgressResponse from(LessonProgress p) {
        return new LessonProgressResponse(
                p.getId().toString(),
                p.getEnrollment().getId().toString(),
                p.getLesson().getId().toString(),
                p.getStatus().name(),
                p.getLastPositionSeconds(),
                p.getStartedAt(),
                p.getCompletedAt(),
                p.getUpdatedAt());
    }
}
