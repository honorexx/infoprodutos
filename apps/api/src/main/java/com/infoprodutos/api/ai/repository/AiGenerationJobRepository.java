package com.infoprodutos.api.ai.repository;

import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.domain.AiJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {
    Optional<AiGenerationJob> findByIdempotencyKey(String idempotencyKey);

    List<AiGenerationJob> findByRequestedByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AiGenerationJob> findAllByOrderByCreatedAtDesc();

    List<AiGenerationJob> findByCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds);

    List<AiGenerationJob> findByStatusInAndStartedAtBefore(List<AiJobStatus> statuses, Instant before);
}
