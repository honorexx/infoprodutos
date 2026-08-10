package com.infoprodutos.api.course;

import com.infoprodutos.api.course.domain.Course;

/** Resolve a URL pública da capa do curso para os DTOs da API. */
public final class CourseCoverUrls {

    private CourseCoverUrls() {}

    public static boolean hasCover(Course course) {
        String raw = course.getCoverImageUrl();
        return raw != null && !raw.isBlank();
    }

    public static boolean isExternalUrl(String raw) {
        if (raw == null) {
            return false;
        }
        String v = raw.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    /**
     * URL consumível pelo frontend:
     * - http(s) externa → retornada como está
     * - storage local → path relativo à API (`/courses/{id}/cover`)
     */
    public static String resolveForApi(Course course) {
        String raw = course.getCoverImageUrl();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (isExternalUrl(raw)) {
            return raw.trim();
        }
        return "/courses/" + course.getId() + "/cover";
    }
}
