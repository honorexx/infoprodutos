package com.infoprodutos.api.ai.provider.mock;

import com.infoprodutos.api.ai.provider.TranscriptionProvider;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptionResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.VideoAssetRef;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Provedor de transcrição de desenvolvimento. Gera um roteiro educacional
 * coerente em pt-BR a partir do título da aula (ou usa texto override).
 * Substituível por Whisper/Azure/etc. sem alterar o orquestrador.
 */
@Component
public class MockTranscriptionProvider implements TranscriptionProvider {

    @Override
    public TranscriptionResult transcribe(VideoAssetRef video, String language) {
        String lang = language == null || language.isBlank() ? "pt-BR" : language;
        String source = video.overrideText() != null && !video.overrideText().isBlank()
                ? video.overrideText().trim()
                : buildLessonScript(video.lessonTitle());

        List<String> paragraphs = splitIntoParagraphs(source);
        List<TranscriptSegmentDto> segments = new ArrayList<>();
        double cursor = 0;
        for (int i = 0; i < paragraphs.size(); i++) {
            String text = paragraphs.get(i).trim();
            if (text.isEmpty()) {
                continue;
            }
            double duration = Math.max(8, Math.min(45, text.split("\\s+").length * 0.55));
            BigDecimal start = BigDecimal.valueOf(cursor).setScale(2, RoundingMode.HALF_UP);
            BigDecimal end = BigDecimal.valueOf(cursor + duration).setScale(2, RoundingMode.HALF_UP);
            segments.add(new TranscriptSegmentDto(
                    segments.size(),
                    start,
                    end,
                    text,
                    topicFor(i, video.lessonTitle())));
            cursor += duration + 1.5;
        }

        String fullText = String.join("\n\n", paragraphs);
        return new TranscriptionResult(fullText, lang, "mock-transcription", List.copyOf(segments));
    }

    private static String buildLessonScript(String title) {
        String topic = (title == null || title.isBlank()) ? "esta aula" : title.trim();
        return """
                Bem-vindos à aula "%s". Nesta etapa vamos organizar os conceitos essenciais \
                de forma prática, para que você consiga aplicar o conteúdo no seu próprio projeto.

                Começamos pelo fundamento: todo bom resultado nasce de um diagnóstico claro. \
                Antes de executar qualquer tática, você precisa entender o problema, o público \
                e a métrica que importa. Sem isso, o esforço vira ruído.

                Em seguida, estruturamos um plano simples em três camadas: atrair atenção \
                qualificada, converter com uma oferta compreensível e reter com entrega \
                consistente. Cada camada tem indicadores próprios e um limite de tempo.

                Um erro comum é pular direto para ferramentas. Ferramentas importam, mas só \
                depois da estratégia. Prefira um processo repetível: hipótese, teste pequeno, \
                medição e ajuste. Isso reduz desperdício e acelera aprendizado.

                Para fechar, revise o checklist da aula: defina o objetivo da semana, escolha \
                uma única métrica principal, execute um teste controlado e registre o que \
                aprendeu. Na próxima aula aprofundamos a execução com exemplos reais.
                """
                .formatted(topic)
                .stripIndent()
                .trim();
    }

    private static List<String> splitIntoParagraphs(String source) {
        String[] parts = source.split("\\n\\s*\\n");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.replaceAll("\\s+", " ").trim();
            if (!cleaned.isEmpty()) {
                out.add(cleaned);
            }
        }
        if (out.isEmpty()) {
            out.add(source.trim());
        }
        return out;
    }

    private static String topicFor(int index, String title) {
        return switch (index) {
            case 0 -> "Introdução";
            case 1 -> "Diagnóstico";
            case 2 -> "Estratégia";
            case 3 -> "Execução";
            default -> title == null || title.isBlank() ? "Consolidação" : title;
        };
    }
}
