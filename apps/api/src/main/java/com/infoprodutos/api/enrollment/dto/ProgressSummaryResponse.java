package com.infoprodutos.api.enrollment.dto;

import java.time.Instant;
import java.util.List;

public record ProgressSummaryResponse(
        String enrollmentId,
        String courseId,
        String courseTitle,
        String courseCoverImageUrl,
        String enrollmentStatus,
        int totalPublishedLessons,
        int completedLessons,
        double courseCompletionPercent,
        boolean canFinishCourse,
        Instant courseCompletedAt,
        boolean canIssueCertificate,
        String certificateId,
        List<ModuleProgressSummary> modules) {

    public record ModuleProgressSummary(
            String moduleId,
            String moduleTitle,
            int orderIndex,
            int totalPublishedLessons,
            int completedLessons,
            double completionPercent,
            List<LessonProgressItem> lessons) {}

    public record LessonProgressItem(
            String lessonId,
            String title,
            int orderIndex,
            Integer durationSeconds,
            String accessType,
            String progressStatus,
            int lastPositionSeconds,
            String currentVideoAssetId) {}
}
