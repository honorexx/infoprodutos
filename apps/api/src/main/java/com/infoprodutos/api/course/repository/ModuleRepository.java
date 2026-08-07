package com.infoprodutos.api.course.repository;

import com.infoprodutos.api.course.domain.Module;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModuleRepository extends JpaRepository<Module, UUID> {

    @Query("select m from Module m where m.id = :id and m.deletedAt is null")
    Optional<Module> findActiveById(@Param("id") UUID id);

    @Query("select m from Module m where m.course.id = :courseId and m.deletedAt is null order by m.orderIndex asc")
    List<Module> findAllActiveByCourseOrderByOrderIndex(@Param("courseId") UUID courseId);

    @Query("select coalesce(max(m.orderIndex), -1) from Module m where m.course.id = :courseId and m.deletedAt is null")
    int findMaxOrderIndexByCourse(@Param("courseId") UUID courseId);
}
