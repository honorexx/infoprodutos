package com.infoprodutos.api.ai.provider.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ProviderDtos {
    private ProviderDtos() {}

    public record VideoAssetRef(
            UUID videoAssetId,
            UUID lessonId,
            String lessonTitle,
            String storageKey,
            String overrideText) {}

    public record TranscriptSegmentDto(
            int sequenceIndex,
            BigDecimal startTimeSeconds,
            BigDecimal endTimeSeconds,
            String text,
            String topic) {}

    public record TranscriptionResult(
            String fullText,
            String language,
            String provider,
            List<TranscriptSegmentDto> segments) {}

    public record QuestionGenerationInput(
            UUID courseId,
            UUID moduleId,
            UUID lessonId,
            String lessonTitle,
            String transcript,
            List<TranscriptSegmentDto> segments,
            int questionCount,
            String language,
            String additionalInstructions) {}

    public record EvidenceDto(String excerpt, BigDecimal startTimeSeconds, BigDecimal endTimeSeconds) {}

    public record OptionDto(String text, boolean correct) {}

    public record GeneratedQuestion(
            String statement,
            List<OptionDto> options,
            String explanation,
            String difficulty,
            String topic,
            EvidenceDto evidence) {}

    public record QuestionGenerationResult(
            String provider,
            String model,
            int inputTokensEstimate,
            int outputTokensEstimate,
            List<GeneratedQuestion> questions) {}

    public record ValidationResult(
            boolean batchAccepted,
            List<GeneratedQuestion> validQuestions,
            List<String> discardReasons) {}

    public record UsageMetrics(
            String provider,
            String model,
            int inputTokens,
            int outputTokens,
            String stage) {}
}
