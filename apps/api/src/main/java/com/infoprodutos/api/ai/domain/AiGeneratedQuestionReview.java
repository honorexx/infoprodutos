package com.infoprodutos.api.ai.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_generated_question_review")
@Getter
@Setter
@NoArgsConstructor
public class AiGeneratedQuestionReview extends BaseEntity {

    @Column(name = "ai_generation_job_id", nullable = false)
    private UUID aiGenerationJobId;

    @Column(name = "question_id", nullable = false, unique = true)
    private UUID questionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawAiPayload;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
