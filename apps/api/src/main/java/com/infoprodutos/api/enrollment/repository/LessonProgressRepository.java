package com.infoprodutos.api.enrollment.repository;

import com.infoprodutos.api.enrollment.domain.LessonProgress;
import com.infoprodutos.api.enrollment.domain.LessonProgressStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(UUID enrollmentId, UUID lessonId);

    List<LessonProgress> findAllByEnrollmentId(UUID enrollmentId);

    long countByEnrollmentIdAndStatus(UUID enrollmentId, LessonProgressStatus status);

    @Query(
            """
            select lp from LessonProgress lp
            join fetch lp.lesson
            where lp.enrollment.id = :enrollmentId
            """)
    List<LessonProgress> findAllByEnrollmentIdWithLesson(@Param("enrollmentId") UUID enrollmentId);
}
