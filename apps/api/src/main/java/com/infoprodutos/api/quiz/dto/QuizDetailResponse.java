package com.infoprodutos.api.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizDetailResponse(
        String id,
        String moduleId,
        String title,
        String status,
        BigDecimal passingScore,
        Integer maxAttempts,
        int publishedQuestionCount,
        List<QuestionStaffResponse> questions) {}
