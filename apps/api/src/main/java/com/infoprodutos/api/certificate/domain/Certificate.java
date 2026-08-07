package com.infoprodutos.api.certificate.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "certificate")
@Getter
@Setter
@NoArgsConstructor
public class Certificate extends AuditableEntity {

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private UUID enrollmentId;

    @Column(name = "student_user_id", nullable = false)
    private UUID studentUserId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "validation_code", nullable = false, unique = true, length = 32)
    private String validationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CertificateStatus status = CertificateStatus.ISSUED;

    @Column(name = "student_name_snapshot", nullable = false, length = 150)
    private String studentNameSnapshot;

    @Column(name = "course_title_snapshot", nullable = false, length = 200)
    private String courseTitleSnapshot;

    @Column(name = "workload_hours_snapshot", nullable = false, precision = 6, scale = 2)
    private BigDecimal workloadHoursSnapshot;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "coordinator_name_snapshot", nullable = false, length = 150)
    private String coordinatorNameSnapshot;

    @Column(name = "chief_vision_officer_name_snapshot", nullable = false, length = 150)
    private String chiefVisionOfficerNameSnapshot;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "validation_url", nullable = false, length = 500)
    private String validationUrl;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
