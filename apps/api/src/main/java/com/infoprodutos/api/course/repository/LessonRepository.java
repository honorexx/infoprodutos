package com.infoprodutos.api.course.repository;

import com.infoprodutos.api.course.domain.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    @Query("select l from Lesson l where l.id = :id and l.deletedAt is null")
    Optional<Lesson> findActiveById(@Param("id") UUID id);

    @Query("select l from Lesson l where l.module.id = :moduleId and l.deletedAt is null order by l.orderIndex asc")
    List<Lesson> findAllActiveByModuleOrderByOrderIndex(@Param("moduleId") UUID moduleId);

    @Query("select coalesce(max(l.orderIndex), -1) from Lesson l where l.module.id = :moduleId and l.deletedAt is null")
    int findMaxOrderIndexByModule(@Param("moduleId") UUID moduleId);
}
