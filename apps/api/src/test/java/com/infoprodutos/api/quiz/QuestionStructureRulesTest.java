package com.infoprodutos.api.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.quiz.domain.QuestionOption;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionStructureRulesTest {

    @Test
    void rejectsWrongOptionCount() {
        assertThatThrownBy(() -> QuestionStructureRules.requireValidManualOptions(List.of(opt(0, true))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("4 alternativas");
    }

    @Test
    void rejectsZeroCorrect() {
        assertThatThrownBy(() -> QuestionStructureRules.requireValidManualOptions(
                        List.of(opt(0, false), opt(1, false), opt(2, false), opt(3, false))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1 alternativa correta");
    }

    @Test
    void rejectsTwoCorrect() {
        assertThatThrownBy(() -> QuestionStructureRules.requireValidManualOptions(
                        List.of(opt(0, true), opt(1, true), opt(2, false), opt(3, false))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1 alternativa correta");
    }

    @Test
    void acceptsExactlyFourWithOneCorrect() {
        QuestionStructureRules.requireValidManualOptions(
                List.of(opt(0, false), opt(1, true), opt(2, false), opt(3, false)));
    }

    @Test
    void scorePercent_edges() {
        assertThat(QuestionStructureRules.scorePercent(0, 4)).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(QuestionStructureRules.scorePercent(4, 4)).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(QuestionStructureRules.scorePercent(1, 4)).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(QuestionStructureRules.scorePercent(2, 3)).isEqualByComparingTo(new BigDecimal("66.67"));
        assertThat(QuestionStructureRules.scorePercent(0, 0)).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    private static QuestionOption opt(int order, boolean correct) {
        QuestionOption o = new QuestionOption();
        o.setText("Opção " + order);
        o.setOrderIndex(order);
        o.setCorrect(correct);
        return o;
    }
}
