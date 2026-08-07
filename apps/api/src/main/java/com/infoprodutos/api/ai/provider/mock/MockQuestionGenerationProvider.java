package com.infoprodutos.api.ai.provider.mock;

import com.infoprodutos.api.ai.provider.QuestionGenerationProvider;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.EvidenceDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.GeneratedQuestion;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.OptionDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationInput;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Gerador mock de questões a partir da transcrição. Produz MCQs com evidência
 * real (substring do segmento), 4 alternativas e 1 correta — adequado para
 * revisão humana e testes sem custo de LLM.
 */
@Component
public class MockQuestionGenerationProvider implements QuestionGenerationProvider {

    private static final String[] DIFFICULTIES = {"EASY", "MEDIUM", "HARD"};

    @Override
    public QuestionGenerationResult generate(QuestionGenerationInput input) {
        List<TranscriptSegmentDto> segments = input.segments();
        if (segments == null || segments.isEmpty()) {
            throw new IllegalStateException("Transcrição sem segmentos para gerar questões.");
        }

        int count = Math.max(1, Math.min(input.questionCount(), 10));
        List<GeneratedQuestion> questions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            TranscriptSegmentDto segment = segments.get(i % segments.size());
            String excerpt = pickExcerpt(segment.text());
            int correctIndex = i % 4;
            String difficulty = DIFFICULTIES[i % DIFFICULTIES.length];
            String topic = segment.topic() != null ? segment.topic() : input.lessonTitle();

            List<OptionDto> options = buildOptions(correctIndex, topic, i);
            String statement = switch (difficulty) {
                case "HARD" ->
                        "Com base na aula \""
                                + nullSafe(input.lessonTitle())
                                + "\", qual afirmação melhor descreve a abordagem recomendada no trecho sobre "
                                + topic.toLowerCase()
                                + "?";
                case "MEDIUM" ->
                        "De acordo com o conteúdo sobre "
                                + topic.toLowerCase()
                                + ", qual prática o professor recomenda priorizar?";
                default ->
                        "Segundo a aula, o que deve acontecer antes de escolher ferramentas?";
            };

            // Ajuste do enunciado fácil para bater com o script padrão
            if ("EASY".equals(difficulty) && i == 0) {
                statement = "Segundo a aula, o que deve acontecer antes de escolher ferramentas?";
            }

            String explanation =
                    "A resposta correta está apoiada no trecho transcrito: \"" + excerpt + "\".";

            questions.add(new GeneratedQuestion(
                    statement,
                    options,
                    explanation,
                    difficulty,
                    topic,
                    new EvidenceDto(excerpt, segment.startTimeSeconds(), segment.endTimeSeconds())));
        }

        int inputTokens = Math.max(50, input.transcript().length() / 4);
        int outputTokens = questions.size() * 120;
        return new QuestionGenerationResult("mock-llm", "mock-v1", inputTokens, outputTokens, questions);
    }

    private static List<OptionDto> buildOptions(int correctIndex, String topic, int seed) {
        String[] candidates = {
            "Fazer um diagnóstico claro do problema, do público e da métrica principal.",
            "Começar imediatamente pelas ferramentas mais populares do mercado.",
            "Ignorar métricas e focar apenas em volume de publicação.",
            "Automatizar tudo antes de validar a hipótese com um teste pequeno."
        };
        // Varia a redação da correta em algumas rodadas
        if (seed % 3 == 1) {
            candidates[0] = "Priorizar um processo repetível: hipótese, teste pequeno, medição e ajuste.";
        } else if (seed % 3 == 2) {
            candidates[0] = "Definir objetivo da semana, uma métrica principal e um teste controlado.";
        }

        List<OptionDto> options = new ArrayList<>(4);
        // Rotate so correctIndex lands on candidates[0] content
        for (int i = 0; i < 4; i++) {
            int source = (i - correctIndex + 4) % 4;
            options.add(new OptionDto(candidates[source], i == correctIndex));
        }
        return options;
    }

    private static String pickExcerpt(String text) {
        if (text == null || text.isBlank()) {
            return "conteúdo da aula";
        }
        String cleaned = text.trim();
        if (cleaned.length() <= 140) {
            return cleaned;
        }
        // Prefer a sentence-like window
        int cut = cleaned.indexOf('.', 60);
        if (cut > 40 && cut < 180) {
            return cleaned.substring(0, cut + 1).trim();
        }
        return cleaned.substring(0, 140).trim();
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "esta aula" : value.trim();
    }
}
