package com.infoprodutos.api.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.infoprodutos.api.ai.provider.dto.ProviderDtos.EvidenceDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.GeneratedQuestion;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.OptionDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructuralAiContentValidatorTest {

    private final StructuralAiContentValidator validator = new StructuralAiContentValidator();

    private final List<TranscriptSegmentDto> segments = List.of(new TranscriptSegmentDto(
            0,
            BigDecimal.ZERO,
            BigDecimal.TEN,
            "Antes de executar qualquer tática, você precisa entender o problema, o público e a métrica.",
            "Diagnóstico"));

    @Test
    void acceptsValidQuestionWithEvidence() {
        var result = validator.validate(
                new QuestionGenerationResult("mock", "v1", 10, 10, List.of(validQuestion())), segments);
        assertTrue(result.batchAccepted());
        assertEquals(1, result.validQuestions().size());
    }

    @Test
    void rejectsWhenNotExactlyFourOptions() {
        GeneratedQuestion q = new GeneratedQuestion(
                "Qual o primeiro passo recomendado na aula?",
                List.of(
                        new OptionDto("Diagnóstico", true),
                        new OptionDto("Ferramentas", false),
                        new OptionDto("Volume", false)),
                "Explicação",
                "EASY",
                "Diagnóstico",
                new EvidenceDto("entender o problema, o público e a métrica", BigDecimal.ZERO, BigDecimal.TEN));
        var result = validator.validate(new QuestionGenerationResult("m", "v", 1, 1, List.of(q)), segments);
        assertFalse(result.batchAccepted());
    }

    @Test
    void rejectsEvidenceNotInTranscript() {
        GeneratedQuestion q = validQuestion();
        GeneratedQuestion bad = new GeneratedQuestion(
                q.statement(),
                q.options(),
                q.explanation(),
                q.difficulty(),
                q.topic(),
                new EvidenceDto("texto inventado que nao existe na aula", BigDecimal.ZERO, BigDecimal.TEN));
        var result = validator.validate(new QuestionGenerationResult("m", "v", 1, 1, List.of(bad)), segments);
        assertFalse(result.batchAccepted());
        assertTrue(result.discardReasons().stream().anyMatch(r -> r.contains("evidência")));
    }

    private GeneratedQuestion validQuestion() {
        return new GeneratedQuestion(
                "Qual o primeiro passo recomendado na aula?",
                List.of(
                        new OptionDto("Entender problema, público e métrica", true),
                        new OptionDto("Começar pelas ferramentas", false),
                        new OptionDto("Ignorar métricas", false),
                        new OptionDto("Automatizar tudo", false)),
                "O diagnóstico vem antes das ferramentas.",
                "EASY",
                "Diagnóstico",
                new EvidenceDto("entender o problema, o público e a métrica", BigDecimal.ZERO, BigDecimal.TEN));
    }
}
