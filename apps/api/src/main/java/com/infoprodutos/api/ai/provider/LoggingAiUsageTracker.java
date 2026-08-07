package com.infoprodutos.api.ai.provider;

import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.UsageMetrics;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingAiUsageTracker implements AiUsageTracker {

    private final AiGenerationJobRepository jobRepository;

    @Override
    @Transactional
    public void recordUsage(UUID jobId, UsageMetrics metrics) {
        // Nunca logar conteúdo da transcrição — apenas métricas agregadas.
        log.info(
                "AI usage job={} provider={} model={} stage={} inTokens={} outTokens={}",
                jobId,
                metrics.provider(),
                metrics.model(),
                metrics.stage(),
                metrics.inputTokens(),
                metrics.outputTokens());

        jobRepository.findById(jobId).ifPresent(job -> {
            Map<String, Object> meta = job.getUsageMetadata() != null
                    ? new HashMap<>(job.getUsageMetadata())
                    : new HashMap<>();
            meta.put("lastProvider", metrics.provider());
            meta.put("lastModel", metrics.model());
            meta.put("lastStage", metrics.stage());
            meta.put("inputTokens", metrics.inputTokens());
            meta.put("outputTokens", metrics.outputTokens());
            int prevIn = ((Number) meta.getOrDefault("totalInputTokens", 0)).intValue();
            int prevOut = ((Number) meta.getOrDefault("totalOutputTokens", 0)).intValue();
            meta.put("totalInputTokens", prevIn + metrics.inputTokens());
            meta.put("totalOutputTokens", prevOut + metrics.outputTokens());
            job.setUsageMetadata(meta);
            jobRepository.save(job);
        });
    }
}
