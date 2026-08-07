package com.infoprodutos.api.quiz;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.quiz.domain.QuestionOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regra Fase 5: questão manual (e publicação) exige exatamente 4 alternativas e exatamente 1 correta.
 */
public final class QuestionStructureRules {

    public static final int REQUIRED_OPTION_COUNT = 4;

    private QuestionStructureRules() {}

    public static void requireValidManualOptions(List<QuestionOption> options) {
        if (options == null || options.size() != REQUIRED_OPTION_COUNT) {
            throw new BadRequestException("A questão deve ter exatamente 4 alternativas.");
        }
        long correctCount = options.stream().filter(QuestionOption::isCorrect).count();
        if (correctCount != 1) {
            throw new BadRequestException("A questão deve ter exatamente 1 alternativa correta.");
        }
        Set<Integer> indexes = new HashSet<>();
        for (QuestionOption opt : options) {
            if (opt.getText() == null || opt.getText().isBlank()) {
                throw new BadRequestException("Texto da alternativa é obrigatório.");
            }
            if (!indexes.add(opt.getOrderIndex())) {
                throw new BadRequestException("orderIndex das alternativas deve ser único.");
            }
        }
    }

    /** Score 0–100 com uma casa decimal. */
    public static java.math.BigDecimal scorePercent(int correct, int total) {
        if (total <= 0) {
            return java.math.BigDecimal.ZERO.setScale(2);
        }
        return java.math.BigDecimal.valueOf(correct * 100.0 / total)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
