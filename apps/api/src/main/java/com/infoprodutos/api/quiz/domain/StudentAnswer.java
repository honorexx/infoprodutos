package com.infoprodutos.api.quiz.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_answer")
@Getter
@Setter
@NoArgsConstructor
public class StudentAnswer extends BaseEntity {

    @Column(name = "quiz_attempt_id", nullable = false)
    private UUID quizAttemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "selected_option_id", nullable = false)
    private UUID selectedOptionId;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt = Instant.now();

    public StudentAnswer(UUID quizAttemptId, UUID questionId, UUID selectedOptionId, boolean correct) {
        this.quizAttemptId = quizAttemptId;
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.correct = correct;
        this.answeredAt = Instant.now();
    }
}
