package com.infoprodutos.api.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizAttemptResponse(
        String id,
        String enrollmentId,
        String quizId,
        int attemptNumber,
        String status,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal score,
        Boolean passed,
        List<AnswerResultItem> answers) {

    public record AnswerResultItem(
            String questionId,
            String statement,
            String selectedOptionId,
            String selectedOptionText,
            boolean correct,
            String correctOptionId,
            String explanation) {}
}
