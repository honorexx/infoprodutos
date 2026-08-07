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
@Table(name = "ai_generation_job")
@Getter
@Setter
@NoArgsConstructor
public class AiGenerationJob extends BaseEntity {

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "video_asset_id")
    private UUID videoAssetId;

    @Column(name = "transcript_id")
    private UUID transcriptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiJobStatus status = AiJobStatus.PENDING;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "requested_question_count", nullable = false)
    private int requestedQuestionCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_distribution", columnDefinition = "jsonb")
    private Map<String, Integer> difficultyDistribution;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "extra_instructions", columnDefinition = "TEXT")
    private String extraInstructions;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "usage_metadata", columnDefinition = "jsonb")
    private Map<String, Object> usageMetadata;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
