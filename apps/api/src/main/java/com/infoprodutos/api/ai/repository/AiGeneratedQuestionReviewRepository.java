package com.infoprodutos.api.ai.repository;

import com.infoprodutos.api.ai.domain.AiGeneratedQuestionReview;
import com.infoprodutos.api.ai.domain.ReviewStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGeneratedQuestionReviewRepository extends JpaRepository<AiGeneratedQuestionReview, UUID> {
    List<AiGeneratedQuestionReview> findByAiGenerationJobIdOrderByCreatedAtAsc(UUID jobId);

    Optional<AiGeneratedQuestionReview> findByQuestionId(UUID questionId);

    long countByAiGenerationJobIdAndReviewStatus(UUID jobId, ReviewStatus status);

    long countByAiGenerationJobId(UUID jobId);
}
