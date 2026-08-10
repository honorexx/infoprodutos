package com.infoprodutos.api.commerce.dto;

import com.infoprodutos.api.commerce.domain.ProductPackage;
import com.infoprodutos.api.course.CourseCoverUrls;
import com.infoprodutos.api.course.domain.Course;
import java.util.Comparator;
import java.util.List;

public record PackageResponse(
        String id,
        String title,
        String slug,
        String description,
        long priceCents,
        String currency,
        boolean active,
        List<PackageCourseSummary> courses) {

    public record PackageCourseSummary(
            String id, String title, String slug, String coverImageUrl, long priceCents) {}

    public static PackageResponse from(ProductPackage pkg) {
        List<PackageCourseSummary> courses = pkg.getCourses().stream()
                .sorted(Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new PackageCourseSummary(
                        c.getId().toString(),
                        c.getTitle(),
                        c.getSlug(),
                        CourseCoverUrls.resolveForApi(c),
                        c.getPriceCents()))
                .toList();
        return new PackageResponse(
                pkg.getId().toString(),
                pkg.getTitle(),
                pkg.getSlug(),
                pkg.getDescription(),
                pkg.getPriceCents(),
                pkg.getCurrency(),
                pkg.isActive(),
                courses);
    }
}
