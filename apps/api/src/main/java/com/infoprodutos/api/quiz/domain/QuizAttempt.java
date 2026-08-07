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
@Table(name = "quiz_attempt")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt extends AuditableEntity {

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuizAttemptStatus status = QuizAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "passed")
    private Boolean passed;

    public QuizAttempt(UUID enrollmentId, UUID quizId, int attemptNumber) {
        this.enrollmentId = enrollmentId;
        this.quizId = quizId;
        this.attemptNumber = attemptNumber;
        this.status = QuizAttemptStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public boolean isMutable() {
        return status == QuizAttemptStatus.IN_PROGRESS;
    }
}
