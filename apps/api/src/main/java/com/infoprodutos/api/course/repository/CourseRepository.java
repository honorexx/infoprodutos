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

    @Query(
            value = "select c from Course c join fetch c.createdBy where c.deletedAt is null",
            countQuery = "select count(c) from Course c where c.deletedAt is null")
    Page<Course> findAllActive(Pageable pageable);

    @Query(
            value = "select c from Course c join fetch c.createdBy where c.deletedAt is null and c.status = :status",
            countQuery = "select count(c) from Course c where c.deletedAt is null and c.status = :status")
    Page<Course> findAllActiveByStatus(@Param("status") CourseStatus status, Pageable pageable);

    @Query(
            value =
                    "select distinct c from Course c join fetch c.createdBy join CourseInstructor ci on ci.course = c "
                            + "where c.deletedAt is null and ci.instructor.id = :instructorId",
            countQuery =
                    "select count(distinct c) from Course c join CourseInstructor ci on ci.course = c "
                            + "where c.deletedAt is null and ci.instructor.id = :instructorId")
    Page<Course> findAllActiveByInstructor(@Param("instructorId") UUID instructorId, Pageable pageable);

    @Query(
            value =
                    "select c from Course c join fetch c.createdBy where c.deletedAt is null "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))",
            countQuery =
                    "select count(c) from Course c where c.deletedAt is null "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))")
    Page<Course> searchAllActive(@Param("q") String q, Pageable pageable);

    @Query(
            value =
                    "select c from Course c join fetch c.createdBy where c.deletedAt is null and c.status = :status "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))",
            countQuery =
                    "select count(c) from Course c where c.deletedAt is null and c.status = :status "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))")
    Page<Course> searchAllActiveByStatus(
            @Param("status") CourseStatus status, @Param("q") String q, Pageable pageable);

    @Query(
            value =
                    "select distinct c from Course c join fetch c.createdBy join CourseInstructor ci on ci.course = c "
                            + "where c.deletedAt is null and ci.instructor.id = :instructorId "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))",
            countQuery =
                    "select count(distinct c) from Course c join CourseInstructor ci on ci.course = c "
                            + "where c.deletedAt is null and ci.instructor.id = :instructorId "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))")
    Page<Course> searchAllActiveByInstructor(
            @Param("instructorId") UUID instructorId, @Param("q") String q, Pageable pageable);

    /**
     * Aluno: cursos matriculados cujo título começa com {@code q}, ou tem uma palavra que começa com {@code q}.
     * Ex.: "dados" encontra "Estrutura de Dados"; "data" não encontra "Digital".
     */
    @Query(
            value =
                    "select distinct c from Course c join fetch c.createdBy join Enrollment e on e.course = c "
                            + "where c.deletedAt is null and e.student.id = :studentId "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))",
            countQuery =
                    "select count(distinct c) from Course c join Enrollment e on e.course = c "
                            + "where c.deletedAt is null and e.student.id = :studentId "
                            + "and (lower(c.title) like lower(concat(:q, '%')) "
                            + "or lower(c.title) like lower(concat('% ', :q, '%')))")
    Page<Course> searchActiveByStudentEnrollment(
            @Param("studentId") UUID studentId, @Param("q") String q, Pageable pageable);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
