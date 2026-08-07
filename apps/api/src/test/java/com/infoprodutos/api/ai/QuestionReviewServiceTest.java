package com.infoprodutos.api.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.repository.AiGeneratedQuestionReviewRepository;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.quiz.domain.Question;
import com.infoprodutos.api.quiz.domain.QuestionOrigin;
import com.infoprodutos.api.quiz.domain.QuestionStatus;
import com.infoprodutos.api.quiz.repository.QuestionOptionRepository;
import com.infoprodutos.api.quiz.repository.QuestionRepository;
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
class QuestionReviewServiceTest {

    @Mock
    private AiJobService aiJobService;

    @Mock
    private AiGenerationJobRepository jobRepository;

    @Mock
    private AiGeneratedQuestionReviewRepository reviewRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository optionRepository;

    @Mock
    private AuditService auditService;

    private QuestionReviewService service;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() throws Exception {
        service = new QuestionReviewService(
                aiJobService, jobRepository, reviewRepository, questionRepository, optionRepository, auditService);
        User user = userWithRole(RoleCode.INSTRUCTOR, "prof@test.local");
        principal = new CustomUserDetails(user);
    }

    @Test
    void publish_rejectsAiGeneratedWithoutApproval() throws Exception {
        UUID questionId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Question question = new Question();
        setId(question, questionId);
        question.setAiGenerationJobId(jobId);
        question.setOrigin(QuestionOrigin.AI_GENERATED);
        question.setStatus(QuestionStatus.DRAFT);
        question.setApprovedByUserId(null);

        AiGenerationJob job = new AiGenerationJob();
        setId(job, jobId);
        job.setCourseId(UUID.randomUUID());

        when(questionRepository.findByIdAndDeletedAtIsNull(questionId)).thenReturn(Optional.of(question));
        when(aiJobService.findOrThrow(jobId)).thenReturn(job);

        assertThatThrownBy(() -> service.publish(questionId, principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("aprovação humana");

        verify(questionRepository, never()).save(any());
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
