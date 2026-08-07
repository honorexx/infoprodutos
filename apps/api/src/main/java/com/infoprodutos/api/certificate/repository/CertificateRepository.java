package com.infoprodutos.api.certificate.repository;

import com.infoprodutos.api.certificate.domain.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByEnrollmentId(UUID enrollmentId);

    Optional<Certificate> findByValidationCode(String validationCode);

    List<Certificate> findByStudentUserIdOrderByIssuedAtDesc(UUID studentUserId);

    boolean existsByEnrollmentId(UUID enrollmentId);

    boolean existsByValidationCode(String validationCode);
}
