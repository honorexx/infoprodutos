package com.infoprodutos.api.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAccessGuardTest {

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    private CourseAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new CourseAccessGuard(courseInstructorRepository);
    }

    @Test
    void superAdmin_canManageAnyCourse() {
        CustomUserDetails principal = principalWithRoles(RoleCode.SUPER_ADMIN);

        assertThat(guard.canManage(UUID.randomUUID(), principal)).isTrue();
    }

    @Test
    void instructor_ownerOfCourse_canManage() {
        UUID courseId = UUID.randomUUID();
        CustomUserDetails principal = principalWithRoles(RoleCode.INSTRUCTOR);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, principal.getId())).thenReturn(true);

        assertThat(guard.canManage(courseId, principal)).isTrue();
    }

    @Test
    void instructor_notOwner_cannotManage() {
        UUID courseId = UUID.randomUUID();
        CustomUserDetails principal = principalWithRoles(RoleCode.INSTRUCTOR);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(any(), any())).thenReturn(false);

        assertThat(guard.canManage(courseId, principal)).isFalse();
    }

    @Test
    void requireManageAccess_deniedForNonOwnerInstructor_throwsForbidden() {
        UUID courseId = UUID.randomUUID();
        CustomUserDetails principal = principalWithRoles(RoleCode.INSTRUCTOR);
        when(courseInstructorRepository.existsByCourseIdAndInstructorId(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> guard.requireManageAccess(courseId, principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void student_cannotManage() {
        CustomUserDetails principal = principalWithRoles(RoleCode.STUDENT);

        assertThat(guard.canManage(UUID.randomUUID(), principal)).isFalse();
    }

    private CustomUserDetails principalWithRoles(String roleCode) {
        User user = new User("Usuario Teste", "user" + UUID.randomUUID() + "@example.com", "hash");
        user.setRoles(Set.of(new Role(roleCode, roleCode)));
        return new CustomUserDetails(user);
    }
}
