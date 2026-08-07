package com.infoprodutos.api.enrollment.repository;

import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Optional<Enrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(UUID studentId, UUID courseId, EnrollmentStatus status);

    @Query(
            """
            select e from Enrollment e
            join fetch e.student
            join fetch e.course
            where e.student.id = :studentId
            order by e.updatedAt desc
            """)
    List<Enrollment> findAllByStudentIdWithDetails(@Param("studentId") UUID studentId);

    @Query(
            """
            select e from Enrollment e
            where (:courseId is null or e.course.id = :courseId)
              and (:studentId is null or e.student.id = :studentId)
              and (:status is null or e.status = :status)
            """)
    Page<Enrollment> findFiltered(
            @Param("courseId") UUID courseId,
            @Param("studentId") UUID studentId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable);

    @Query(
            """
            select e from Enrollment e
            join fetch e.student
            join fetch e.course
            where e.id = :id
            """)
    Optional<Enrollment> findByIdWithDetails(@Param("id") UUID id);
}
