package com.infoprodutos.api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonAccessType;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentAccessGuardTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseAccessGuard courseAccessGuard;

    private EnrollmentAccessGuard guard;
    private User student;
    private Course course;
    private Lesson enrolledLesson;
    private Lesson previewLesson;

    @BeforeEach
    void setUp() throws Exception {
        guard = new EnrollmentAccessGuard(enrollmentRepository, courseAccessGuard);
        student = userWithRole(RoleCode.STUDENT, "aluno@test.local");
        course = new Course("Curso", "curso", student);
        course.setStatus(CourseStatus.PUBLISHED);
        setId(course, UUID.randomUUID());
        Module module = new Module(course, "M", 0);
        setId(module, UUID.randomUUID());

        enrolledLesson = new Lesson(module, "Restrita", 0);
        enrolledLesson.setAccessType(LessonAccessType.ENROLLED_ONLY);
        enrolledLesson.setStatus(LessonStatus.PUBLISHED);
        setId(enrolledLesson, UUID.randomUUID());

        previewLesson = new Lesson(module, "Preview", 1);
        previewLesson.setAccessType(LessonAccessType.FREE_PREVIEW);
        previewLesson.setStatus(LessonStatus.PUBLISHED);
        setId(previewLesson, UUID.randomUUID());
    }

    @Test
    void freePreview_allowsWithoutEnrollment() {
        CustomUserDetails principal = new CustomUserDetails(student);
        when(courseAccessGuard.canManage(course.getId(), principal)).thenReturn(false);

        assertThat(guard.canAccessLessonContent(previewLesson, principal)).isTrue();
    }

    @Test
    void enrolledOnly_requiresActiveEnrollment() {
        CustomUserDetails principal = new CustomUserDetails(student);
        when(courseAccessGuard.canManage(course.getId(), principal)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                        student.getId(), course.getId(), EnrollmentStatus.ACTIVE))
                .thenReturn(false);

        assertThat(guard.canAccessLessonContent(enrolledLesson, principal)).isFalse();
        assertThatThrownBy(() -> guard.requireLessonContentAccess(enrolledLesson, principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void enrolledOnly_allowsWithActiveEnrollment() {
        CustomUserDetails principal = new CustomUserDetails(student);
        when(courseAccessGuard.canManage(course.getId(), principal)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                        student.getId(), course.getId(), EnrollmentStatus.ACTIVE))
                .thenReturn(true);

        assertThat(guard.canAccessLessonContent(enrolledLesson, principal)).isTrue();
    }

    private static User userWithRole(String roleCode, String email) throws Exception {
        Role role = new Role(roleCode, roleCode);
        setId(role, UUID.randomUUID());
        User user = new User("Test", email, "hash");
        setId(user, UUID.randomUUID());
        user.setRoles(Set.of(role));
        return user;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Class<?> c = entity.getClass();
        Field idField = null;
        while (c != null) {
            try {
                idField = c.getDeclaredField("id");
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        if (idField == null) {
            throw new IllegalStateException("id field not found");
        }
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
