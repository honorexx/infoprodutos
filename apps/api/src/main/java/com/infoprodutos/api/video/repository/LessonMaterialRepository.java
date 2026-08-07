package com.infoprodutos.api.video.repository;

import com.infoprodutos.api.video.domain.LessonMaterial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, UUID> {
    List<LessonMaterial> findByLessonIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID lessonId);

    Optional<LessonMaterial> findByIdAndDeletedAtIsNull(UUID id);
}
