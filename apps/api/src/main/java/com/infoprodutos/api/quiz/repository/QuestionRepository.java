package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.Question;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findByAiGenerationJobIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID jobId);

    Optional<Question> findByIdAndDeletedAtIsNull(UUID id);

    List<Question> findByLessonIdAndDeletedAtIsNull(UUID lessonId);

    List<Question> findByQuizIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID quizId);

    long countByAiGenerationJobIdAndDeletedAtIsNull(UUID jobId);

    long countByQuizIdAndStatusAndDeletedAtIsNull(UUID quizId, com.infoprodutos.api.quiz.domain.QuestionStatus status);
}
