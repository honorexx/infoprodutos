package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.StudentAnswer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, UUID> {

    List<StudentAnswer> findByQuizAttemptId(UUID quizAttemptId);

    Optional<StudentAnswer> findByQuizAttemptIdAndQuestionId(UUID quizAttemptId, UUID questionId);

    boolean existsByQuestionId(UUID questionId);

    long countByQuizAttemptIdAndCorrectTrue(UUID quizAttemptId);

    @Modifying(clearAutomatically = true)
    @Query(
            """
            delete from StudentAnswer sa
            where sa.quizAttemptId in (
              select a.id from QuizAttempt a where a.enrollmentId = :enrollmentId
            )
            """)
    int deleteByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);
}
