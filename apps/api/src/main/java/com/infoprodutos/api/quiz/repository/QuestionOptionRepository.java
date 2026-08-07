package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.QuestionOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {
    List<QuestionOption> findByQuestionIdOrderByOrderIndexAsc(UUID questionId);

    void deleteByQuestionId(UUID questionId);
}
