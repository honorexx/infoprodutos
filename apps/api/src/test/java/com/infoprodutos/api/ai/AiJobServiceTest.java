package com.infoprodutos.api.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.ai.config.AiProperties;
import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.domain.AiJobStatus;
import com.infoprodutos.api.ai.dto.AiJobResponse;
import com.infoprodutos.api.ai.dto.CreateAiJobRequest;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.video.repository.VideoAssetRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class AiJobServiceTest {

    @Mock
    private AiGenerationJobRepository jobRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private CourseAccessGuard accessGuard;

    @Mock
    private CourseInstructorRepository courseInstructorRepository;

    @Mock
    private VideoAssetRepository videoAssetRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private AiJobWorker aiJobWorker;

    @Mock
    private AuditService auditService;

    @Mock
    private Environment environment;

    private AiProperties aiProperties;
    private AiJobService service;
    private User instructor;
    private CustomUserDetails principal;
    private Lesson lesson;

    @BeforeEach
    void setUp() throws Exception {
        aiProperties = new AiProperties();
        aiProperties.setMaxAttempts(3);
        service = new AiJobService(
                jobRepository,
                lessonService,
                accessGuard,
                courseInstructorRepository,
                videoAssetRepository,
                lessonRepository,
                aiJobWorker,
                aiProperties,
                auditService,
                environment);

        instructor = userWithRole(RoleCode.INSTRUCTOR, "prof@test.local");
        principal = new CustomUserDetails(instructor);
        Course course = new Course("Curso", "curso", instructor);
        setId(course, UUID.randomUUID());
        Module module = new Module(course, "Módulo", 0);
        setId(module, UUID.randomUUID());
        lesson = new Lesson(module, "Aula", 0);
        setId(lesson, UUID.randomUUID());
        lesson.setCurrentVideoAssetId(UUID.randomUUID());
    }

    @Test
    void create_returnsExistingJobForSameIdempotencyKey() throws Exception {
        AiGenerationJob existing = new AiGenerationJob();
        setId(existing, UUID.randomUUID());
        existing.setLessonId(lesson.getId());
        existing.setCourseId(lesson.getModule().getCourse().getId());
        existing.setModuleId(lesson.getModule().getId());
        existing.setStatus(AiJobStatus.PENDING);
        existing.setIdempotencyKey("key-1");
        existing.setLanguage("pt-BR");
        existing.setRequestedQuestionCount(5);
        existing.setRequestedByUserId(instructor.getId());

        when(lessonService.findActiveOrThrow(lesson.getId())).thenReturn(lesson);
        when(jobRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        AiJobResponse response =
                service.create(lesson.getId(), new CreateAiJobRequest("key-1", 5, "pt-BR", null, null), principal);

        assertThat(response.id()).isEqualTo(existing.getId().toString());
        verify(jobRepository, never()).save(any());
        verify(aiJobWorker, never()).processAsync(any());
    }

    @Test
    void resume_rejectsWhenNotFailed() throws Exception {
        AiGenerationJob job = new AiGenerationJob();
        setId(job, UUID.randomUUID());
        job.setCourseId(lesson.getModule().getCourse().getId());
        job.setStatus(AiJobStatus.COMPLETED);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.resume(job.getId(), principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void resume_rejectsWhenMaxAttemptsReached() throws Exception {
        AiGenerationJob job = new AiGenerationJob();
        setId(job, UUID.randomUUID());
        job.setCourseId(lesson.getModule().getCourse().getId());
        job.setStatus(AiJobStatus.FAILED);
        job.setAttemptCount(3);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.resume(job.getId(), principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Limite de tentativas");
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
