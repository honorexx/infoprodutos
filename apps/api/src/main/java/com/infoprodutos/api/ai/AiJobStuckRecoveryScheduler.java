package com.infoprodutos.api.ai;

import com.infoprodutos.api.ai.config.AiProperties;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Recupera jobs de IA travados além de {@code app.ai.stuck-timeout-minutes}
 * (Fase 6). Usa {@code FOR UPDATE SKIP LOCKED} no repositório.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiJobStuckRecoveryScheduler {

    private final AiGenerationJobRepository jobRepository;
    private final AiJobWorker aiJobWorker;
    private final AiProperties aiProperties;

    @Scheduled(fixedDelayString = "${app.ai.stuck-check-interval-ms:60000}")
    @Transactional
    public void reclaimStuckJobs() {
        Instant before = Instant.now().minus(aiProperties.getStuckTimeoutMinutes(), ChronoUnit.MINUTES);
        List<UUID> stuck = jobRepository.lockStuckJobIds(before, aiProperties.getMaxAttempts(), 20);
        if (stuck.isEmpty()) {
            return;
        }
        jobRepository.resetJobsToPending(stuck);
        log.info("Reclaiming {} stuck AI job(s)", stuck.size());
        List<UUID> toProcess = List.copyOf(stuck);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    toProcess.forEach(aiJobWorker::processAsync);
                }
            });
        } else {
            toProcess.forEach(aiJobWorker::processAsync);
        }
    }
}
