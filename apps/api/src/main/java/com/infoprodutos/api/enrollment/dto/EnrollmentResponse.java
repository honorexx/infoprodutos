package com.infoprodutos.api.enrollment.dto;

import com.infoprodutos.api.enrollment.domain.Enrollment;
import java.time.Instant;

public record EnrollmentResponse(
        String id,
        String studentUserId,
        String studentName,
        String studentEmail,
        String courseId,
        String courseTitle,
        String status,
        Instant startedAt,
        Instant expiresAt,
        Instant completedAt,
        String grantedByUserId,
        Instant createdAt,
        Instant updatedAt) {

    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId().toString(),
                e.getStudent().getId().toString(),
                e.getStudent().getName(),
                e.getStudent().getEmail(),
                e.getCourse().getId().toString(),
                e.getCourse().getTitle(),
                e.getStatus().name(),
                e.getStartedAt(),
                e.getExpiresAt(),
                e.getCompletedAt(),
                e.getGrantedByUserId() != null ? e.getGrantedByUserId().toString() : null,
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
