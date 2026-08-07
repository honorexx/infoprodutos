package com.infoprodutos.api.quiz.dto;

import java.util.List;

/** Visão do aluno antes/durante a tentativa — sem revelar a alternativa correta. */
public record QuizTakeResponse(
        String quizId,
        String moduleId,
        String title,
        Integer maxAttempts,
        int attemptsUsed,
        boolean canStartNewAttempt,
        String inProgressAttemptId,
        List<QuestionTakeItem> questions) {

    public record QuestionTakeItem(
            String id, String statement, int orderIndex, List<OptionTakeItem> options) {}

    public record OptionTakeItem(String id, String text, int orderIndex) {}
}
