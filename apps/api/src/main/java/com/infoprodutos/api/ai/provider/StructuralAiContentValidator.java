package com.infoprodutos.api.ai.provider;

import com.infoprodutos.api.ai.provider.dto.ProviderDtos.GeneratedQuestion;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.OptionDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.ValidationResult;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validação estrutural equivalente a um JSON Schema + regras de negócio.
 * Nunca confia apenas no modelo: descarta questões inválidas individualmente.
 */
@Component
public class StructuralAiContentValidator implements AiContentValidator {

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    @Override
    public ValidationResult validate(QuestionGenerationResult result, List<TranscriptSegmentDto> segments) {
        List<String> discardReasons = new ArrayList<>();
        List<GeneratedQuestion> valid = new ArrayList<>();

        if (result == null || result.questions() == null || result.questions().isEmpty()) {
            return new ValidationResult(false, List.of(), List.of("Lote vazio ou ausente."));
        }

        Set<String> statements = new HashSet<>();
        int index = 0;
        for (GeneratedQuestion q : result.questions()) {
            index++;
            String reason = validateOne(q, segments, statements);
            if (reason != null) {
                discardReasons.add("Q" + index + ": " + reason);
            } else {
                valid.add(q);
                statements.add(normalize(q.statement()));
            }
        }

        boolean accepted = !valid.isEmpty();
        if (!accepted && discardReasons.isEmpty()) {
            discardReasons.add("Nenhuma questão válida no lote.");
        }
        return new ValidationResult(accepted, List.copyOf(valid), List.copyOf(discardReasons));
    }

    private String validateOne(
            GeneratedQuestion q, List<TranscriptSegmentDto> segments, Set<String> seenStatements) {
        if (q == null) {
            return "questão nula";
        }
        if (isBlank(q.statement()) || q.statement().trim().length() < 10 || q.statement().length() > 500) {
            return "enunciado inválido";
        }
        if (seenStatements.contains(normalize(q.statement()))) {
            return "enunciado duplicado no lote";
        }
        if (q.options() == null || q.options().size() != 4) {
            return "deve ter exatamente 4 alternativas";
        }
        long correctCount = q.options().stream().filter(OptionDto::correct).count();
        if (correctCount != 1) {
            return "deve ter exatamente 1 alternativa correta";
        }
        Set<String> optionTexts = new HashSet<>();
        for (OptionDto opt : q.options()) {
            if (isBlank(opt.text()) || opt.text().length() > 500) {
                return "alternativa vazia ou longa demais";
            }
            if (!optionTexts.add(normalize(opt.text()))) {
                return "alternativas duplicadas";
            }
        }
        if (isBlank(q.explanation())) {
            return "explicação obrigatória";
        }
        if (q.difficulty() == null || !DIFFICULTIES.contains(q.difficulty().toUpperCase(Locale.ROOT))) {
            return "dificuldade inválida";
        }
        if (q.evidence() == null || isBlank(q.evidence().excerpt())) {
            return "evidência obrigatória";
        }
        if (!evidenceMatchesTranscript(q.evidence().excerpt(), segments)) {
            return "evidência incompatível com a transcrição";
        }
        return null;
    }

    private boolean evidenceMatchesTranscript(String excerpt, List<TranscriptSegmentDto> segments) {
        String needle = normalize(excerpt);
        if (needle.length() < 8) {
            return false;
        }
        for (TranscriptSegmentDto segment : segments) {
            if (normalize(segment.text()).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String n = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return n;
    }
}
