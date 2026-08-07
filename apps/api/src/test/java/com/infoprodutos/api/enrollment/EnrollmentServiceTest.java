package com.infoprodutos.api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.CourseService;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.dto.CreateEnrollmentRequest;
import com.infoprodutos.api.enrollment.dto.EnrollmentResponse;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
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
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseAccessGuard courseAccessGuard;

    @Mock
    private AuditService auditService;

    private EnrollmentService enrollmentService;

    private User instructor;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() throws Exception {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                userRepository,
                courseService,
                courseAccessGuard,
                auditService,
                new ObjectMapper());

        instructor = userWithRole(RoleCode.INSTRUCTOR, "prof@test.local");
        student = userWithRole(RoleCode.STUDENT, "aluno@test.local");
        course = new Course("Curso", "curso", instructor);
        setId(course, UUID.randomUUID());
    }

    @Test
    void grant_createsNewEnrollment() throws Exception {
        when(courseService.findActiveOrThrow(course.getId())).thenReturn(course);
        when(userRepository.findActiveByEmailIgnoreCase("aluno@test.local")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId()))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            setId(e, UUID.randomUUID());
            return e;
        });
        when(enrollmentRepository.findByIdWithDetails(any())).thenAnswer(inv -> {
            Enrollment e = new Enrollment(student, course, instructor.getId());
            setId(e, inv.getArgument(0));
            return Optional.of(e);
        });

        CustomUserDetails principal = new CustomUserDetails(instructor);
        EnrollmentResponse response = enrollmentService.grant(
                new CreateEnrollmentRequest(course.getId(), null, "aluno@test.local"), principal);

        assertThat(response.studentEmail()).isEqualTo("aluno@test.local");
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(auditService).record(eq(instructor.getId()), eq("ENROLLMENT_GRANTED"), eq("Enrollment"), any(), any());
    }

    @Test
    void grant_reactivatesSuspendedEnrollment() throws Exception {
        Enrollment existing = new Enrollment(student, course, instructor.getId());
        setId(existing, UUID.randomUUID());
        existing.setStatus(EnrollmentStatus.SUSPENDED);

        when(courseService.findActiveOrThrow(course.getId())).thenReturn(course);
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId()))
                .thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.findByIdWithDetails(existing.getId())).thenReturn(Optional.of(existing));

        CustomUserDetails principal = new CustomUserDetails(instructor);
        EnrollmentResponse response = enrollmentService.grant(
                new CreateEnrollmentRequest(course.getId(), student.getId(), null), principal);

        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(auditService).record(eq(instructor.getId()), eq("ENROLLMENT_REACTIVATED"), eq("Enrollment"), any(), any());
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
