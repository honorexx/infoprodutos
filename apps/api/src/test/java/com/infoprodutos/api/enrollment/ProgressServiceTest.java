package com.infoprodutos.api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.domain.LessonProgress;
import com.infoprodutos.api.enrollment.domain.LessonProgressStatus;
import com.infoprodutos.api.enrollment.dto.ProgressHeartbeatRequest;
import com.infoprodutos.api.enrollment.repository.LessonProgressRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private CourseAccessGuard courseAccessGuard;

    @Mock
    private com.infoprodutos.api.certificate.CertificateEligibilityService certificateEligibilityService;

    @Mock
    private com.infoprodutos.api.certificate.repository.CertificateRepository certificateRepository;

    private ProgressService progressService;

    private User student;
    private User otherStudent;
    private Course course;
    private Lesson lesson;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() throws Exception {
        progressService = new ProgressService(
                enrollmentService,
                lessonProgressRepository,
                lessonService,
                lessonRepository,
                moduleRepository,
                courseAccessGuard,
                certificateEligibilityService,
                certificateRepository);

        student = userWithRole(RoleCode.STUDENT, "aluno@test.local");
        otherStudent = userWithRole(RoleCode.STUDENT, "outro@test.local");
        course = new Course("Curso", "curso", student);
        setId(course, UUID.randomUUID());
        Module module = new Module(course, "Módulo", 0);
        setId(module, UUID.randomUUID());
        lesson = new Lesson(module, "Aula", 0);
        lesson.setStatus(LessonStatus.PUBLISHED);
        lesson.setDurationSeconds(100);
        setId(lesson, UUID.randomUUID());

        enrollment = new Enrollment(student, course, student.getId());
        setId(enrollment, UUID.randomUUID());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
    }

    @Test
    void heartbeat_marksCompletedAt90Percent() throws Exception {
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        when(lessonService.findActiveOrThrow(lesson.getId())).thenReturn(lesson);

        LessonProgress progress = new LessonProgress(enrollment, lesson);
        setId(progress, UUID.randomUUID());
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson.getId()))
                .thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomUserDetails principal = new CustomUserDetails(student);
        var response = progressService.heartbeat(
                enrollment.getId(), lesson.getId(), new ProgressHeartbeatRequest(90), principal);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.lastPositionSeconds()).isEqualTo(90);
    }

    @Test
    void heartbeat_doesNotRegressFromCompleted() throws Exception {
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        when(lessonService.findActiveOrThrow(lesson.getId())).thenReturn(lesson);

        LessonProgress progress = new LessonProgress(enrollment, lesson);
        setId(progress, UUID.randomUUID());
        progress.setStatus(LessonProgressStatus.COMPLETED);
        progress.setLastPositionSeconds(95);
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson.getId()))
                .thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomUserDetails principal = new CustomUserDetails(student);
        var response = progressService.heartbeat(
                enrollment.getId(), lesson.getId(), new ProgressHeartbeatRequest(10), principal);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.lastPositionSeconds()).isEqualTo(95);
    }

    @Test
    void complete_manualWorksWithoutVideoDuration() throws Exception {
        lesson.setDurationSeconds(null);
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        when(lessonService.findActiveOrThrow(lesson.getId())).thenReturn(lesson);

        LessonProgress progress = new LessonProgress(enrollment, lesson);
        setId(progress, UUID.randomUUID());
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lesson.getId()))
                .thenReturn(Optional.of(progress));
        when(lessonProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomUserDetails principal = new CustomUserDetails(student);
        var response = progressService.complete(enrollment.getId(), lesson.getId(), principal);

        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void start_rejectsOtherStudentsEnrollment() {
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        CustomUserDetails other = new CustomUserDetails(otherStudent);

        assertThatThrownBy(() -> progressService.start(enrollment.getId(), lesson.getId(), other))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(lessonProgressRepository, never()).save(any());
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
