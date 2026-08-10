package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.QuizAttempt;
import com.infoprodutos.api.quiz.domain.QuizAttemptStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("delete from QuizAttempt a where a.enrollmentId = :enrollmentId")
    int deleteByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    List<QuizAttempt> findByEnrollmentIdAndQuizIdOrderByAttemptNumberAsc(UUID enrollmentId, UUID quizId);

    List<QuizAttempt> findByEnrollmentIdOrderByStartedAtDesc(UUID enrollmentId);

    Optional<QuizAttempt> findByIdAndEnrollmentId(UUID id, UUID enrollmentId);

    long countByEnrollmentIdAndQuizId(UUID enrollmentId, UUID quizId);

    Optional<QuizAttempt> findFirstByEnrollmentIdAndQuizIdAndStatusOrderByAttemptNumberDesc(
            UUID enrollmentId, UUID quizId, QuizAttemptStatus status);

    @Query("select coalesce(max(a.attemptNumber), 0) from QuizAttempt a where a.enrollmentId = :enrollmentId and a.quizId = :quizId")
    int findMaxAttemptNumber(@Param("enrollmentId") UUID enrollmentId, @Param("quizId") UUID quizId);

    @Query(
            """
            select max(a.score) from QuizAttempt a
             where a.enrollmentId = :enrollmentId
               and a.quizId = :quizId
               and a.status = com.infoprodutos.api.quiz.domain.QuizAttemptStatus.GRADED
               and a.score is not null
            """)
    BigDecimal findBestGradedScore(
            @Param("enrollmentId") UUID enrollmentId, @Param("quizId") UUID quizId);
}
