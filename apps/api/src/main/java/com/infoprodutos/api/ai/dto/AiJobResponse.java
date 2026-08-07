package com.infoprodutos.api.ai.dto;

import com.infoprodutos.api.ai.domain.AiGenerationJob;
import java.util.Map;

public record AiJobResponse(
        String id,
        String courseId,
        String moduleId,
        String lessonId,
        String videoAssetId,
        String transcriptId,
        String status,
        String provider,
        String model,
        int requestedQuestionCount,
        String language,
        String idempotencyKey,
        int attemptCount,
        String errorMessage,
        Map<String, Object> usageMetadata,
        String createdAt,
        String startedAt,
        String completedAt) {

    public static AiJobResponse from(AiGenerationJob job) {
        return new AiJobResponse(
                job.getId().toString(),
                job.getCourseId().toString(),
                job.getModuleId().toString(),
                job.getLessonId().toString(),
                job.getVideoAssetId() != null ? job.getVideoAssetId().toString() : null,
                job.getTranscriptId() != null ? job.getTranscriptId().toString() : null,
                job.getStatus().name(),
                job.getProvider(),
                job.getModel(),
                job.getRequestedQuestionCount(),
                job.getLanguage(),
                job.getIdempotencyKey(),
                job.getAttemptCount(),
                job.getErrorMessage(),
                job.getUsageMetadata(),
                job.getCreatedAt() != null ? job.getCreatedAt().toString() : null,
                job.getStartedAt() != null ? job.getStartedAt().toString() : null,
                job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
    }
}
