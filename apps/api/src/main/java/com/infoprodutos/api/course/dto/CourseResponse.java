package com.infoprodutos.api.course.dto;

import com.infoprodutos.api.course.CourseCoverUrls;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseInstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CourseResponse(
        String id,
        String title,
        String slug,
        String description,
        String coverImageUrl,
        BigDecimal workloadHours,
        long priceCents,
        String currency,
        String status,
        BigDecimal minCompletionPercentage,
        BigDecimal minPassingScore,
        boolean certificateEnabled,
        Integer maxQuizAttempts,
        String createdByUserId,
        String createdByName,
        List<InstructorSummary> instructors,
        Instant publishedAt,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {

    public record InstructorSummary(String userId, String name, boolean primary) {}

    public static CourseResponse from(Course course, List<CourseInstructor> instructors) {
        return new CourseResponse(
                course.getId().toString(),
                course.getTitle(),
                course.getSlug(),
                course.getDescription(),
                CourseCoverUrls.resolveForApi(course),
                course.getWorkloadHours(),
                course.getPriceCents(),
                course.getCurrency(),
                course.getStatus().name(),
                course.getMinCompletionPercentage(),
                course.getMinPassingScore(),
                course.isCertificateEnabled(),
                course.getMaxQuizAttempts(),
                course.getCreatedBy().getId().toString(),
                course.getCreatedBy().getName(),
                instructors.stream()
                        .map(ci -> new InstructorSummary(
                                ci.getInstructor().getId().toString(), ci.getInstructor().getName(), ci.isPrimary()))
                        .toList(),
                course.getPublishedAt(),
                course.getArchivedAt(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }
}
