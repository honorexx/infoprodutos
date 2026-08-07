package com.infoprodutos.api.enrollment;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonAccessType;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Acesso a conteúdo de aula/vídeo: dono/admin, matrícula ACTIVE, ou FREE_PREVIEW
 * em curso publicado.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentAccessGuard {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseAccessGuard courseAccessGuard;

    public boolean hasActiveEnrollment(UUID studentUserId, UUID courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                studentUserId, courseId, EnrollmentStatus.ACTIVE);
    }

    public boolean canAccessLessonContent(Lesson lesson, CustomUserDetails principal) {
        UUID courseId = lesson.getModule().getCourse().getId();
        if (courseAccessGuard.canManage(courseId, principal)) {
            return true;
        }
        if (isFreePreviewPublished(lesson)) {
            return true;
        }
        return hasActiveEnrollment(principal.getId(), courseId);
    }

    public void requireLessonContentAccess(Lesson lesson, CustomUserDetails principal) {
        if (!canAccessLessonContent(lesson, principal)) {
            throw new ForbiddenOperationException("Sem permissão para acessar este conteúdo. Matrícula ativa necessária.");
        }
    }

    public void requireManageEnrollment(UUID courseId, CustomUserDetails principal) {
        courseAccessGuard.requireManageAccess(courseId, principal);
    }

    public boolean isStaff(CustomUserDetails principal) {
        return principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)
                || principal.getRoleCodes().contains(RoleCode.INSTRUCTOR);
    }

    private static boolean isFreePreviewPublished(Lesson lesson) {
        return lesson.getAccessType() == LessonAccessType.FREE_PREVIEW
                && lesson.getStatus() == LessonStatus.PUBLISHED
                && lesson.getModule().getCourse().getStatus() == CourseStatus.PUBLISHED
                && lesson.getDeletedAt() == null;
    }
}
