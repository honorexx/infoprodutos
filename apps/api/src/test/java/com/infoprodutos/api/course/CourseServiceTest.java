package com.infoprodutos.api.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.dto.CourseCreateRequest;
import com.infoprodutos.api.course.dto.CourseResponse;
import com.infoprodutos.api.course.dto.CourseUpdateRequest;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.course.repository.CourseRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseAccessGuard accessGuard;

    @Mock
    private AuditService auditService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService =
                new CourseService(courseRepository, courseInstructorRepository, userRepository, accessGuard, auditService, new ObjectMapper());
    }

    @Test
    void create_asInstructor_generatesSlugAndRegistersAsOwner() {
        User instructor = userWithRole(RoleCode.INSTRUCTOR);
        CustomUserDetails principal = new CustomUserDetails(instructor);
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(instructor));
        when(courseRepository.existsBySlugAndDeletedAtIsNull(any())).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));
        when(courseInstructorRepository.findByCourseId(any())).thenReturn(List.of());

        CourseResponse response =
                courseService.create(new CourseCreateRequest("Curso de Java", null, "desc", null), principal);

        assertThat(response.slug()).isEqualTo("curso-de-java");
        assertThat(response.status()).isEqualTo("DRAFT");
        verify(courseInstructorRepository).save(any());
        verify(auditService).record(any(), eq("COURSE_CREATED"), any(), any(), any());
    }

    @Test
    void create_slugConflict_appendsSuffix() {
        User instructor = userWithRole(RoleCode.INSTRUCTOR);
        CustomUserDetails principal = new CustomUserDetails(instructor);
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(instructor));
        when(courseRepository.existsBySlugAndDeletedAtIsNull("curso-de-java")).thenReturn(true);
        when(courseRepository.existsBySlugAndDeletedAtIsNull("curso-de-java-2")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> withGeneratedId(inv.getArgument(0)));
        when(courseInstructorRepository.findByCourseId(any())).thenReturn(List.of());

        CourseResponse response =
                courseService.create(new CourseCreateRequest("Curso de Java", null, null, null), principal);

        assertThat(response.slug()).isEqualTo("curso-de-java-2");
    }

    @Test
    void update_deniedByAccessGuard_propagatesForbidden() {
        Course course = sampleCourse();
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.INSTRUCTOR));
        doThrow(new ForbiddenOperationException("sem permissao"))
                .when(accessGuard)
                .requireManageAccess(course.getId(), principal);

        CourseUpdateRequest request =
                new CourseUpdateRequest(
                        "Novo título", null, null, java.math.BigDecimal.TEN, null, null, true, null);

        assertThatThrownBy(() -> courseService.update(course.getId(), request, principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void update_notFound_throwsNotFound() {
        when(courseRepository.findActiveById(any())).thenReturn(Optional.empty());
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.SUPER_ADMIN));
        CourseUpdateRequest request =
                new CourseUpdateRequest("Titulo", null, null, java.math.BigDecimal.ONE, null, null, true, null);

        assertThatThrownBy(() -> courseService.update(UUID.randomUUID(), request, principal))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void publish_fromDraft_setsPublishedAtAndStatus() {
        Course course = sampleCourse();
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.SUPER_ADMIN));

        courseService.publish(course.getId(), principal);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(course.getPublishedAt()).isNotNull();
    }

    @Test
    void publish_alreadyPublished_isIdempotent() {
        Course course = sampleCourse();
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.SUPER_ADMIN));

        courseService.publish(course.getId(), principal);

        verify(courseRepository, never()).save(any());
    }

    @Test
    void publish_archivedCourse_throwsConflict() {
        Course course = sampleCourse();
        course.setStatus(CourseStatus.ARCHIVED);
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.SUPER_ADMIN));

        assertThatThrownBy(() -> courseService.publish(course.getId(), principal)).isInstanceOf(ConflictException.class);
    }

    @Test
    void unpublish_fromPublished_setsDraft() {
        Course course = sampleCourse();
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.SUPER_ADMIN));

        courseService.unpublish(course.getId(), principal);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void archive_fromDraft_setsArchivedAtAndStatus() {
        Course course = sampleCourse();
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.SUPER_ADMIN));

        courseService.archive(course.getId(), principal);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
        assertThat(course.getArchivedAt()).isNotNull();
    }

    @Test
    void get_studentViewingDraftCourse_throwsForbidden() {
        Course course = sampleCourse();
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.STUDENT));

        assertThatThrownBy(() -> courseService.get(course.getId(), principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void get_studentViewingPublishedCourse_succeeds() {
        Course course = sampleCourse();
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findActiveById(course.getId())).thenReturn(Optional.of(course));
        when(courseInstructorRepository.findByCourseId(course.getId())).thenReturn(List.of());
        CustomUserDetails principal = new CustomUserDetails(userWithRole(RoleCode.STUDENT));

        CourseResponse response = courseService.get(course.getId(), principal);

        assertThat(response.status()).isEqualTo("PUBLISHED");
    }

    private Course sampleCourse() {
        User creator = userWithRole(RoleCode.INSTRUCTOR);
        Course course = new Course("Curso Teste", "curso-teste", creator);
        setId(course);
        return course;
    }

    private User userWithRole(String roleCode) {
        User user = new User("Usuario", "user" + UUID.randomUUID() + "@example.com", "hash");
        user.setRoles(Set.of(new Role(roleCode, roleCode)));
        setId(user);
        return user;
    }

    /** Atribui um id via reflexão, já que @GeneratedValue só é preenchido pelo Hibernate real. */
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
