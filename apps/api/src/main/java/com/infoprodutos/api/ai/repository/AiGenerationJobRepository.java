package com.infoprodutos.api.ai.repository;

import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.domain.AiJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {
    Optional<AiGenerationJob> findByIdempotencyKey(String idempotencyKey);

    List<AiGenerationJob> findByRequestedByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AiGenerationJob> findAllByOrderByCreatedAtDesc();

    List<AiGenerationJob> findByCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds);

    List<AiGenerationJob> findByStatusInAndStartedAtBefore(List<AiJobStatus> statuses, Instant before);

    /**
     * Reivindica jobs travados em estados intermediários (Fase 6).
     * {@code FOR UPDATE SKIP LOCKED} evita disputa entre instâncias.
     */
    @Query(
            value =
                    """
                    SELECT id FROM ai_generation_job
                    WHERE status IN ('TRANSCRIBING', 'TRANSCRIBED', 'GENERATING')
                      AND started_at IS NOT NULL
                      AND started_at < :before
                      AND attempt_count < :maxAttempts
                    ORDER BY started_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<UUID> lockStuckJobIds(
            @Param("before") Instant before,
            @Param("maxAttempts") int maxAttempts,
            @Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update AiGenerationJob j
               set j.status = com.infoprodutos.api.ai.domain.AiJobStatus.PENDING,
                   j.errorMessage = null,
                   j.completedAt = null
             where j.id in :ids
            """)
    int resetJobsToPending(@Param("ids") List<UUID> ids);
}
