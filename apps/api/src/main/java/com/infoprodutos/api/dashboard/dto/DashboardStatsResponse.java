package com.infoprodutos.api.dashboard.dto;

import java.util.List;

public record DashboardStatsResponse(
        String roleView,
        int year,
        StudentStats student,
        InstructorStats instructor,
        AdminStats admin,
        List<ActivityPoint> activitySeries) {

    public record StudentStats(
            long enrolledCourses,
            long startedCourses,
            long completedCourses,
            double averageProgressPercent) {}

    public record InstructorStats(
            long ownedCourses,
            long publishedCourses,
            long enrolledStudents,
            long activeStudentsLast7Days) {}

    public record AdminStats(
            long totalStudents,
            long publishedCourses,
            long totalCourses,
            long totalEnrollments,
            long activeEnrollments) {}

    /** Ponto mensal do gráfico de aprendizado (0–100). */
    public record ActivityPoint(String label, int month, double value) {}
}
