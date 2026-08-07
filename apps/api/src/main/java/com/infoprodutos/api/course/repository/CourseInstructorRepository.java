package com.infoprodutos.api.course.repository;

import com.infoprodutos.api.course.domain.CourseInstructor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, UUID> {

    @Query("select ci from CourseInstructor ci join fetch ci.instructor where ci.course.id = :courseId")
    List<CourseInstructor> findByCourseId(@Param("courseId") UUID courseId);

    boolean existsByCourseIdAndInstructorId(UUID courseId, UUID instructorId);

    @Query("select ci.course.id from CourseInstructor ci where ci.instructor.id = :instructorId")
    List<UUID> findCourseIdsByInstructorId(@Param("instructorId") UUID instructorId);
}
