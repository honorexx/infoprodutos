package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.QuizAttempt;
import com.infoprodutos.api.quiz.domain.QuizAttemptStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByEnrollmentIdAndQuizIdOrderByAttemptNumberAsc(UUID enrollmentId, UUID quizId);

    List<QuizAttempt> findByEnrollmentIdOrderByStartedAtDesc(UUID enrollmentId);

    Optional<QuizAttempt> findByIdAndEnrollmentId(UUID id, UUID enrollmentId);

    long countByEnrollmentIdAndQuizId(UUID enrollmentId, UUID quizId);

    Optional<QuizAttempt> findFirstByEnrollmentIdAndQuizIdAndStatusOrderByAttemptNumberDesc(
            UUID enrollmentId, UUID quizId, QuizAttemptStatus status);

    @Query("select coalesce(max(a.attemptNumber), 0) from QuizAttempt a where a.enrollmentId = :enrollmentId and a.quizId = :quizId")
    int findMaxAttemptNumber(@Param("enrollmentId") UUID enrollmentId, @Param("quizId") UUID quizId);
}
