package com.infoprodutos.api.ai.provider.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infoprodutos.api.ai.provider.StructuralAiContentValidator;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationInput;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockQuestionGenerationProviderTest {

    @Test
    void generatesQuestionsThatPassValidator() {
        var segments = List.of(
                new TranscriptSegmentDto(
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(20),
                        "Antes de executar qualquer tática, você precisa entender o problema, o público e a métrica que importa.",
                        "Diagnóstico"),
                new TranscriptSegmentDto(
                        1,
                        BigDecimal.valueOf(21),
                        BigDecimal.valueOf(40),
                        "Prefira um processo repetível: hipótese, teste pequeno, medição e ajuste.",
                        "Execução"));

        var provider = new MockQuestionGenerationProvider();
        var result = provider.generate(new QuestionGenerationInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fundamentos",
                segments.get(0).text() + "\n" + segments.get(1).text(),
                segments,
                3,
                "pt-BR",
                null));

        assertEquals(3, result.questions().size());
        result.questions().forEach(q -> {
            assertEquals(4, q.options().size());
            assertEquals(1, q.options().stream().filter(o -> o.correct()).count());
        });

        var validation = new StructuralAiContentValidator().validate(result, segments);
        assertTrue(validation.batchAccepted(), () -> validation.discardReasons().toString());
    }
}
