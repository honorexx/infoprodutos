package com.infoprodutos.api.quiz.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz")
@Getter
@Setter
@NoArgsConstructor
public class Quiz extends AuditableEntity {

    @Column(name = "module_id", nullable = false, unique = true)
    private UUID moduleId;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "passing_score")
    private BigDecimal passingScore;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
