package com.infoprodutos.api.commerce.repository;

import com.infoprodutos.api.commerce.domain.ProductPackage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPackageRepository extends JpaRepository<ProductPackage, UUID> {

    @Query("""
            select distinct p from ProductPackage p
            left join fetch p.courses c
            left join fetch c.createdBy
            where p.deletedAt is null and p.active = true
            order by p.title
            """)
    List<ProductPackage> findAllActiveWithCourses();

    @Query("""
            select distinct p from ProductPackage p
            left join fetch p.courses c
            left join fetch c.createdBy
            where p.deletedAt is null
            order by p.title
            """)
    List<ProductPackage> findAllNotDeletedWithCourses();

    @Query("""
            select distinct p from ProductPackage p
            left join fetch p.courses c
            left join fetch c.createdBy
            where p.id = :id and p.deletedAt is null
            """)
    Optional<ProductPackage> findActiveByIdWithCourses(@Param("id") UUID id);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
