package com.infoprodutos.api.dashboard;

import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.repository.CourseRepository;
import com.infoprodutos.api.dashboard.dto.DashboardStatsResponse;
import com.infoprodutos.api.dashboard.dto.DashboardStatsResponse.ActivityPoint;
import com.infoprodutos.api.dashboard.dto.DashboardStatsResponse.AdminStats;
import com.infoprodutos.api.dashboard.dto.DashboardStatsResponse.InstructorStats;
import com.infoprodutos.api.dashboard.dto.DashboardStatsResponse.StudentStats;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.domain.LessonProgressStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.enrollment.repository.LessonProgressRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final double ACTIVITY_GAIN = 8.0;
    private static final double ACTIVITY_DECAY = 3.0;

    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseRepository courseRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats(CustomUserDetails principal) {
        int year = LocalDate.now(ZoneOffset.UTC).getYear();
        boolean isAdmin = principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN);
        boolean isInstructor = principal.getRoleCodes().contains(RoleCode.INSTRUCTOR);

        if (isAdmin) {
            return new DashboardStatsResponse(
                    "ADMIN",
                    year,
                    null,
                    null,
                    buildAdminStats(),
                    buildPlatformActivitySeries(year));
        }
        if (isInstructor) {
            return new DashboardStatsResponse(
                    "INSTRUCTOR",
                    year,
                    null,
                    buildInstructorStats(principal.getId()),
                    null,
                    buildInstructorActivitySeries(principal.getId(), year));
        }
        return new DashboardStatsResponse(
                "STUDENT",
                year,
                buildStudentStats(principal.getId()),
                null,
                null,
                buildStudentActivitySeries(principal.getId(), year));
    }

    private StudentStats buildStudentStats(UUID studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByStudentIdWithDetails(studentId).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .toList();

        long enrolled = enrollments.size();
        long started = 0;
        long completed = 0;
        double progressSum = 0;

        for (Enrollment enrollment : enrollments) {
            long done = lessonProgressRepository.countByEnrollmentIdAndStatus(
                    enrollment.getId(), LessonProgressStatus.COMPLETED);
            long inProgress = lessonProgressRepository.countByEnrollmentIdAndStatus(
                    enrollment.getId(), LessonProgressStatus.IN_PROGRESS);
            boolean hasStarted = done + inProgress > 0 || enrollment.getCompletedAt() != null;
            if (hasStarted) {
                started++;
            }
            if (enrollment.getCompletedAt() != null) {
                completed++;
            }

            long totalLessons = countPublishedLessons(enrollment.getCourse().getId());
            double pct = totalLessons == 0 ? 0 : (100.0 * done) / totalLessons;
            progressSum += Math.min(100.0, pct);
        }

        double avg = enrolled == 0 ? 0 : Math.round((progressSum / enrolled) * 10.0) / 10.0;
        return new StudentStats(enrolled, started, completed, avg);
    }

    private InstructorStats buildInstructorStats(UUID instructorId) {
        var courses = courseRepository
                .findAllActiveByInstructor(instructorId, Pageable.unpaged())
                .getContent();
        long owned = courses.size();
        long published = courses.stream().filter(c -> c.getStatus() == CourseStatus.PUBLISHED).count();

        Set<UUID> courseIds = new HashSet<>();
        courses.forEach(c -> courseIds.add(c.getId()));

        long enrolledStudents = 0;
        long activeLast7 = 0;
        if (!courseIds.isEmpty()) {
            enrolledStudents = countDistinctStudentsInCourses(courseIds);
            Instant since = Instant.now().minusSeconds(7L * 24 * 3600);
            activeLast7 = countActiveStudentsSince(courseIds, since);
        }

        return new InstructorStats(owned, published, enrolledStudents, activeLast7);
    }

    private AdminStats buildAdminStats() {
        long students = countUsersWithRole(RoleCode.STUDENT);
        long published = courseRepository
                .findAllActiveByStatus(CourseStatus.PUBLISHED, Pageable.unpaged())
                .getTotalElements();
        long totalCourses = courseRepository.findAllActive(Pageable.unpaged()).getTotalElements();
        long totalEnrollments = enrollmentRepository.count();
        long activeEnrollments = countEnrollmentsByStatus(EnrollmentStatus.ACTIVE);
        return new AdminStats(students, published, totalCourses, totalEnrollments, activeEnrollments);
    }

    private List<ActivityPoint> buildStudentActivitySeries(UUID studentId, int year) {
        @SuppressWarnings("unchecked")
        List<Instant> events = entityManager
                .createQuery(
                        """
                        select coalesce(lp.completedAt, lp.startedAt, lp.updatedAt)
                        from LessonProgress lp
                        where lp.enrollment.student.id = :studentId
                          and coalesce(lp.completedAt, lp.startedAt, lp.updatedAt) is not null
                        """)
                .setParameter("studentId", studentId)
                .getResultList();
        return buildMomentumSeries(events, year);
    }

    private List<ActivityPoint> buildInstructorActivitySeries(UUID instructorId, int year) {
        @SuppressWarnings("unchecked")
        List<Instant> events = entityManager
                .createQuery(
                        """
                        select coalesce(lp.completedAt, lp.startedAt, lp.updatedAt)
                        from LessonProgress lp
                        join CourseInstructor ci on ci.course = lp.enrollment.course
                        where ci.instructor.id = :instructorId
                          and coalesce(lp.completedAt, lp.startedAt, lp.updatedAt) is not null
                        """)
                .setParameter("instructorId", instructorId)
                .getResultList();
        return buildMomentumSeries(events, year);
    }

    private List<ActivityPoint> buildPlatformActivitySeries(int year) {
        @SuppressWarnings("unchecked")
        List<Instant> events = entityManager
                .createQuery(
                        """
                        select coalesce(lp.completedAt, lp.startedAt, lp.updatedAt)
                        from LessonProgress lp
                        where coalesce(lp.completedAt, lp.startedAt, lp.updatedAt) is not null
                        """)
                .getResultList();
        return buildMomentumSeries(events, year);
    }

    /**
     * Série mensal cumulativa: dias com estudo sobem o índice; dias parados descem
     * (mínimo 0, máximo 100). Espelha o ritmo de aprendizado ao longo do ano.
     */
    List<ActivityPoint> buildMomentumSeries(List<Instant> events, int year) {
        Set<LocalDate> activeDays = new HashSet<>();
        for (Instant instant : events) {
            if (instant == null) {
                continue;
            }
            LocalDate day = instant.atZone(ZoneOffset.UTC).toLocalDate();
            if (day.getYear() == year) {
                activeDays.add(day);
            }
        }

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (today.getYear() == year && today.isBefore(end)) {
            end = today;
        }

        double score = 0;
        double[] monthMax = new double[12];
        boolean[] monthSeen = new boolean[12];

        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (activeDays.contains(day)) {
                score = Math.min(100.0, score + ACTIVITY_GAIN);
            } else if (score > 0) {
                score = Math.max(0.0, score - ACTIVITY_DECAY);
            }
            int m = day.getMonthValue() - 1;
            monthMax[m] = Math.max(monthMax[m], score);
            monthSeen[m] = true;
        }

        List<ActivityPoint> points = new ArrayList<>(12);
        Locale pt = Locale.forLanguageTag("pt-BR");
        for (int i = 0; i < 12; i++) {
            String label = LocalDate.of(year, i + 1, 1)
                    .getMonth()
                    .getDisplayName(TextStyle.SHORT, pt);
            double value = monthSeen[i] ? Math.round(monthMax[i] * 10.0) / 10.0 : 0;
            points.add(new ActivityPoint(label, i + 1, value));
        }
        return points;
    }

    private long countPublishedLessons(UUID courseId) {
        Long count = entityManager
                .createQuery(
                        """
                        select count(l) from Lesson l
                        join l.module m
                        where m.course.id = :courseId
                          and l.deletedAt is null
                          and m.deletedAt is null
                          and l.status = com.infoprodutos.api.course.domain.LessonStatus.PUBLISHED
                          and m.status = com.infoprodutos.api.course.domain.ModuleStatus.PUBLISHED
                        """,
                        Long.class)
                .setParameter("courseId", courseId)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    private long countDistinctStudentsInCourses(Set<UUID> courseIds) {
        Long count = entityManager
                .createQuery(
                        """
                        select count(distinct e.student.id) from Enrollment e
                        where e.course.id in :ids and e.status = :status
                        """,
                        Long.class)
                .setParameter("ids", courseIds)
                .setParameter("status", EnrollmentStatus.ACTIVE)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    private long countActiveStudentsSince(Set<UUID> courseIds, Instant since) {
        Long count = entityManager
                .createQuery(
                        """
                        select count(distinct lp.enrollment.student.id) from LessonProgress lp
                        where lp.enrollment.course.id in :ids
                          and lp.enrollment.status = :status
                          and lp.updatedAt >= :since
                          and lp.status <> :notStarted
                        """,
                        Long.class)
                .setParameter("ids", courseIds)
                .setParameter("status", EnrollmentStatus.ACTIVE)
                .setParameter("since", since)
                .setParameter("notStarted", LessonProgressStatus.NOT_STARTED)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    private long countUsersWithRole(String roleCode) {
        Long count = entityManager
                .createQuery(
                        """
                        select count(distinct u.id) from User u
                        join u.roles r
                        where u.deletedAt is null and r.code = :code
                        """,
                        Long.class)
                .setParameter("code", roleCode)
                .getSingleResult();
        return count == null ? 0 : count;
    }

    private long countEnrollmentsByStatus(EnrollmentStatus status) {
        Long count = entityManager
                .createQuery(
                        "select count(e) from Enrollment e where e.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
        return count == null ? 0 : count;
    }
}
