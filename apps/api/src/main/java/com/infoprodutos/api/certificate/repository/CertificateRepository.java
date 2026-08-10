package com.infoprodutos.api.certificate.repository;

import com.infoprodutos.api.certificate.domain.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByEnrollmentId(UUID enrollmentId);

    Optional<Certificate> findByValidationCode(String validationCode);

    List<Certificate> findByStudentUserIdOrderByIssuedAtDesc(UUID studentUserId);

    boolean existsByEnrollmentId(UUID enrollmentId);

    boolean existsByValidationCode(String validationCode);

    @Modifying(clearAutomatically = true)
    @Query("delete from Certificate c where c.enrollmentId = :enrollmentId")
    int deleteByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);
}
