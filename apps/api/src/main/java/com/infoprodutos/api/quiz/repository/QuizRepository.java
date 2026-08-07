package com.infoprodutos.api.quiz.repository;

import com.infoprodutos.api.quiz.domain.Quiz;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    Optional<Quiz> findByModuleIdAndDeletedAtIsNull(UUID moduleId);
}
