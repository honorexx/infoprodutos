package com.infoprodutos.api.course.dto;

import com.infoprodutos.api.course.CourseCoverUrls;
import com.infoprodutos.api.course.domain.Course;
import java.math.BigDecimal;
import java.time.Instant;

public record CourseSummaryResponse(
        String id,
        String title,
        String slug,
        String coverImageUrl,
        BigDecimal workloadHours,
        long priceCents,
        String currency,
        String status,
        String createdByName,
        Instant createdAt,
        Instant updatedAt) {

    public static CourseSummaryResponse from(Course course) {
        return new CourseSummaryResponse(
                course.getId().toString(),
                course.getTitle(),
                course.getSlug(),
                CourseCoverUrls.resolveForApi(course),
                course.getWorkloadHours(),
                course.getPriceCents(),
                course.getCurrency(),
                course.getStatus().name(),
                course.getCreatedBy().getName(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }
}
