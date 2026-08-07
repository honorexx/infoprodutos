package com.infoprodutos.api.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonAccessType;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.dto.LessonRequest;
import com.infoprodutos.api.course.dto.LessonResponse;
import com.infoprodutos.api.course.dto.ReorderRequest;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ModuleService moduleService;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseAccessGuard accessGuard;

    @Mock
    private com.infoprodutos.api.audit.AuditService auditService;

    private LessonService lessonService;

    @BeforeEach
    void setUp() {
        lessonService = new LessonService(lessonRepository, moduleService, courseService, accessGuard, auditService);
    }

    @Test
    void create_assignsNextOrderIndexAndAccessType() {
        Course course = sampleCourse();
        Module module = sampleModule(course);
        when(moduleService.findActiveOrThrow(module.getId())).thenReturn(module);
        when(lessonRepository.findMaxOrderIndexByModule(module.getId())).thenReturn(-1);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));
        CustomUserDetails principal = principal(RoleCode.SUPER_ADMIN);

        LessonResponse response = lessonService.create(
                module.getId(), new LessonRequest("Aula 1", null, 120, LessonAccessType.FREE_PREVIEW), principal);

        assertThat(response.orderIndex()).isEqualTo(0);
        assertThat(response.accessType()).isEqualTo("FREE_PREVIEW");
    }

    @Test
    void create_deniedByAccessGuard_throwsForbidden() {
        Course course = sampleCourse();
        Module module = sampleModule(course);
        when(moduleService.findActiveOrThrow(module.getId())).thenReturn(module);
        CustomUserDetails principal = principal(RoleCode.INSTRUCTOR);
        doThrow(new ForbiddenOperationException("sem permissao"))
                .when(accessGuard)
                .requireManageAccess(course.getId(), principal);

        assertThatThrownBy(() -> lessonService.create(
                        module.getId(), new LessonRequest("Aula", null, null, LessonAccessType.ENROLLED_ONLY), principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void reorder_withMismatchedIds_throwsBadRequest() {
        Course course = sampleCourse();
        Module module = sampleModule(course);
        Lesson lesson1 = sampleLesson(module, 0);
        Lesson lesson2 = sampleLesson(module, 1);
        when(moduleService.findActiveOrThrow(module.getId())).thenReturn(module);
        when(lessonRepository.findAllActiveByModuleOrderByOrderIndex(module.getId())).thenReturn(List.of(lesson1, lesson2));
        CustomUserDetails principal = principal(RoleCode.SUPER_ADMIN);

        ReorderRequest request = new ReorderRequest(List.of(lesson1.getId(), UUID.randomUUID()));

        assertThatThrownBy(() -> lessonService.reorder(module.getId(), request, principal))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reorder_validPermutation_reassignsOrderIndex() {
        Course course = sampleCourse();
        Module module = sampleModule(course);
        Lesson lesson1 = sampleLesson(module, 0);
        Lesson lesson2 = sampleLesson(module, 1);
        when(moduleService.findActiveOrThrow(module.getId())).thenReturn(module);
        when(lessonRepository.findAllActiveByModuleOrderByOrderIndex(module.getId())).thenReturn(List.of(lesson1, lesson2));
        CustomUserDetails principal = principal(RoleCode.SUPER_ADMIN);

        lessonService.reorder(module.getId(), new ReorderRequest(List.of(lesson2.getId(), lesson1.getId())), principal);

        assertThat(lesson2.getOrderIndex()).isEqualTo(0);
        assertThat(lesson1.getOrderIndex()).isEqualTo(1);
    }

    private Course sampleCourse() {
        User creator = principal(RoleCode.INSTRUCTOR).getUser();
        Course course = new Course("Curso Teste", "curso-teste", creator);
        setId(course);
        return course;
    }

    private Module sampleModule(Course course) {
        Module module = new Module(course, "Módulo", 0);
        setId(module);
        return module;
    }

    private Lesson sampleLesson(Module module, int order) {
        Lesson lesson = new Lesson(module, "Aula " + order, order);
        setId(lesson);
        return lesson;
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
