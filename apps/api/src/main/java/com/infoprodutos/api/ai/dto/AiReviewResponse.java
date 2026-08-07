package com.infoprodutos.api.ai.dto;

import java.util.List;
import java.util.Map;

public record AiReviewResponse(
        String reviewId,
        String questionId,
        String jobId,
        String reviewStatus,
        String statement,
        String explanation,
        String difficulty,
        String topic,
        String questionStatus,
        List<OptionView> options,
        Map<String, Object> evidence,
        Map<String, Object> rawAiPayload,
        String reviewedAt) {

    public record OptionView(String id, String text, boolean correct, int orderIndex) {}
}
