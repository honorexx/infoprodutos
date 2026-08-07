package com.infoprodutos.api.course.repository;

import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    // "join fetch createdBy" evita LazyInitializationException ao montar as respostas
    // fora de transação explícita (spring.jpa.open-in-view=false - ver docs/DECISIONS.md).
    @Query("select c from Course c join fetch c.createdBy where c.id = :id and c.deletedAt is null")
    Optional<Course> findActiveById(@Param("id") UUID id);

    @Query("select c from Course c join fetch c.createdBy where c.deletedAt is null")
    Page<Course> findAllActive(Pageable pageable);

    @Query("select c from Course c join fetch c.createdBy where c.deletedAt is null and c.status = :status")
    Page<Course> findAllActiveByStatus(@Param("status") CourseStatus status, Pageable pageable);

    @Query(
            "select distinct c from Course c join fetch c.createdBy join CourseInstructor ci on ci.course = c "
                    + "where c.deletedAt is null and ci.instructor.id = :instructorId")
    Page<Course> findAllActiveByInstructor(@Param("instructorId") UUID instructorId, Pageable pageable);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
