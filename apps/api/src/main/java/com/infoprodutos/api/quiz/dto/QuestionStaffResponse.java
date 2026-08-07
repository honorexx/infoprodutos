package com.infoprodutos.api.quiz.dto;

import java.time.Instant;
import java.util.List;

public record QuestionStaffResponse(
        String id,
        String quizId,
        String lessonId,
        String statement,
        String explanation,
        String difficulty,
        String topic,
        String status,
        String origin,
        int orderIndex,
        Instant createdAt,
        List<OptionStaffResponse> options) {

    public record OptionStaffResponse(String id, String text, boolean correct, int orderIndex) {}
}
