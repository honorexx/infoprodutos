package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.StudentAnswer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, UUID> {

    List<StudentAnswer> findByQuizAttemptId(UUID quizAttemptId);

    Optional<StudentAnswer> findByQuizAttemptIdAndQuestionId(UUID quizAttemptId, UUID questionId);

    boolean existsByQuestionId(UUID questionId);

    long countByQuizAttemptIdAndCorrectTrue(UUID quizAttemptId);
}
