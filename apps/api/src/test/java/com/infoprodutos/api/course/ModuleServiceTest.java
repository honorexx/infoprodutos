package com.infoprodutos.api.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.dto.ModuleRequest;
import com.infoprodutos.api.course.dto.ModuleResponse;
import com.infoprodutos.api.course.dto.ReorderRequest;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseAccessGuard accessGuard;

    @Mock
    private com.infoprodutos.api.audit.AuditService auditService;

    private ModuleService moduleService;

    @BeforeEach
    void setUp() {
        moduleService = new ModuleService(moduleRepository, lessonRepository, courseService, accessGuard, auditService);
    }

    @Test
    void create_assignsNextOrderIndex() {
        Course course = sampleCourse();
        when(courseService.findActiveOrThrow(course.getId())).thenReturn(course);
        when(moduleRepository.findMaxOrderIndexByCourse(course.getId())).thenReturn(1);
        when(moduleRepository.save(any(Module.class))).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));
        CustomUserDetails principal = principal(RoleCode.SUPER_ADMIN);

        ModuleResponse response = moduleService.create(course.getId(), new ModuleRequest("Módulo 2", null), principal);

        assertThat(response.orderIndex()).isEqualTo(2);
    }

    @Test
    void create_deniedByAccessGuard_throwsForbidden() {
        Course course = sampleCourse();
        when(courseService.findActiveOrThrow(course.getId())).thenReturn(course);
        CustomUserDetails principal = principal(RoleCode.INSTRUCTOR);
        doThrow(new ForbiddenOperationException("sem permissao")).when(accessGuard).requireManageAccess(course.getId(), principal);

        assertThatThrownBy(() -> moduleService.create(course.getId(), new ModuleRequest("Módulo", null), principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void reorder_withMismatchedIds_throwsBadRequest() {
        Course course = sampleCourse();
        Module module1 = sampleModule(course, 0);
        Module module2 = sampleModule(course, 1);
        when(moduleRepository.findAllActiveByCourseOrderByOrderIndex(course.getId())).thenReturn(List.of(module1, module2));
        CustomUserDetails principal = principal(RoleCode.SUPER_ADMIN);

        ReorderRequest request = new ReorderRequest(List.of(module1.getId(), UUID.randomUUID()));

        assertThatThrownBy(() -> moduleService.reorder(course.getId(), request, principal))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorder_validPermutation_reassignsOrderIndex() {
        Course course = sampleCourse();
        Module module1 = sampleModule(course, 0);
        Module module2 = sampleModule(course, 1);
        when(moduleRepository.findAllActiveByCourseOrderByOrderIndex(course.getId())).thenReturn(List.of(module1, module2));
        CustomUserDetails principal = principal(RoleCode.SUPER_ADMIN);

        moduleService.reorder(course.getId(), new ReorderRequest(List.of(module2.getId(), module1.getId())), principal);

        assertThat(module2.getOrderIndex()).isEqualTo(0);
        assertThat(module1.getOrderIndex()).isEqualTo(1);
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(moduleRepository.findActiveById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> moduleService.delete(UUID.randomUUID(), principal(RoleCode.SUPER_ADMIN)))
                .isInstanceOf(com.infoprodutos.api.common.exception.NotFoundException.class);
    }

    private Course sampleCourse() {
        User creator = principal(RoleCode.INSTRUCTOR).getUser();
        Course course = new Course("Curso Teste", "curso-teste", creator);
        setId(course);
        return course;
    }

    private Module sampleModule(Course course, int order) {
        Module module = new Module(course, "Módulo " + order, order);
        setId(module);
        return module;
    }

    private CustomUserDetails principal(String roleCode) {
        User user = new User("Usuario", "user" + UUID.randomUUID() + "@example.com", "hash");
        user.setRoles(Set.of(new Role(roleCode, roleCode)));
        setId(user);
        return new CustomUserDetails(user);
    }

    private void setId(com.infoprodutos.api.common.domain.BaseEntity entity) {
        if (entity.getId() != null) {
            return;
        }
        try {
            var field = com.infoprodutos.api.common.domain.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends com.infoprodutos.api.common.domain.BaseEntity> T withGeneratedId(T entity) {
        setId(entity);
        return entity;
    }
}
