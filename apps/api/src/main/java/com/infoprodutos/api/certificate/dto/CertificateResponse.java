package com.infoprodutos.api.certificate.dto;

import com.infoprodutos.api.certificate.domain.Certificate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CertificateResponse(
        String id,
        String enrollmentId,
        String courseId,
        String studentName,
        String courseTitle,
        BigDecimal workloadHours,
        LocalDate completionDate,
        Instant issuedAt,
        String validationCode,
        String validationUrl,
        String status,
        String coordinatorName,
        String chiefVisionOfficerName) {

    public static CertificateResponse from(Certificate c) {
        return new CertificateResponse(
                c.getId().toString(),
                c.getEnrollmentId().toString(),
                c.getCourseId().toString(),
                c.getStudentNameSnapshot(),
                c.getCourseTitleSnapshot(),
                c.getWorkloadHoursSnapshot(),
                c.getCompletionDate(),
                c.getIssuedAt(),
                c.getValidationCode(),
                c.getValidationUrl(),
                c.getStatus().name(),
                c.getCoordinatorNameSnapshot(),
                c.getChiefVisionOfficerNameSnapshot());
    }
}
