package com.infoprodutos.api.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.quiz.domain.Question;
import com.infoprodutos.api.quiz.domain.QuestionOption;
import com.infoprodutos.api.quiz.domain.QuestionStatus;
import com.infoprodutos.api.quiz.domain.Quiz;
import com.infoprodutos.api.quiz.domain.QuizAttempt;
import com.infoprodutos.api.quiz.domain.QuizAttemptStatus;
import com.infoprodutos.api.quiz.domain.QuizStatus;
import com.infoprodutos.api.quiz.domain.StudentAnswer;
import com.infoprodutos.api.quiz.dto.QuizAttemptResponse;
import com.infoprodutos.api.quiz.dto.SubmitAnswerRequest;
import com.infoprodutos.api.quiz.repository.QuestionOptionRepository;
import com.infoprodutos.api.quiz.repository.QuestionRepository;
import com.infoprodutos.api.quiz.repository.QuizAttemptRepository;
import com.infoprodutos.api.quiz.repository.QuizRepository;
import com.infoprodutos.api.quiz.repository.StudentAnswerRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private CourseAccessGuard courseAccessGuard;

    private AttemptService attemptService;

    private User student;
    private Course course;
    private Module module;
    private Quiz quiz;
    private Enrollment enrollment;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() throws Exception {
        attemptService = new AttemptService(
                quizRepository,
                questionRepository,
                questionOptionRepository,
                quizAttemptRepository,
                studentAnswerRepository,
                enrollmentRepository,
                moduleRepository,
                courseAccessGuard);

        student = userWithRole(RoleCode.STUDENT, "aluno@test.local");
        principal = new CustomUserDetails(student);
        course = new Course("Curso", "curso", student);
        setId(course, UUID.randomUUID());
        course.setMaxQuizAttempts(2);
        course.setMinPassingScore(new BigDecimal("70.00"));
        module = new Module(course, "Módulo", 0);
        setId(module, UUID.randomUUID());
        quiz = new Quiz();
        quiz.setModuleId(module.getId());
        quiz.setTitle("Quiz");
        quiz.setStatus(QuizStatus.PUBLISHED);
        setId(quiz, UUID.randomUUID());
        enrollment = new Enrollment(student, course, student.getId());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        setId(enrollment, UUID.randomUUID());
    }

    @Test
    void startAttempt_rejectsWhenMaxAttemptsReached() {
        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(moduleRepository.findActiveById(module.getId())).thenReturn(Optional.of(module));
        when(enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId()))
                .thenReturn(Optional.of(enrollment));
        when(quizAttemptRepository.findFirstByEnrollmentIdAndQuizIdAndStatusOrderByAttemptNumberDesc(
                        enrollment.getId(), quiz.getId(), QuizAttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(quizAttemptRepository.countByEnrollmentIdAndQuizId(enrollment.getId(), quiz.getId()))
                .thenReturn(2L);

        assertThatThrownBy(() -> attemptService.startAttempt(quiz.getId(), principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Limite de tentativas");

        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    void answer_rejectsWhenAttemptAlreadyGraded() throws Exception {
        QuizAttempt attempt = new QuizAttempt(enrollment.getId(), quiz.getId(), 1);
        setId(attempt, UUID.randomUUID());
        attempt.setStatus(QuizAttemptStatus.GRADED);

        when(quizAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(enrollmentRepository.findByIdWithDetails(enrollment.getId())).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> attemptService.answer(
                        attempt.getId(),
                        new SubmitAnswerRequest(UUID.randomUUID(), UUID.randomUUID()),
                        principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("já finalizada");
    }

    @Test
    void submit_scoreZeroPercent() throws Exception {
        assertSubmitScore("0.00", false, false, false);
    }

    @Test
    void submit_scoreHundredPercent() throws Exception {
        assertSubmitScore("100.00", true, true, true);
    }

    @Test
    void submit_scorePartial() throws Exception {
        assertSubmitScore("50.00", false, true, false);
    }

    private void assertSubmitScore(String expectedScore, boolean passed, boolean q1Correct, boolean q2Correct)
            throws Exception {
        clearInvocations(quizAttemptRepository);

        Question q1 = publishedQuestion(quiz.getId(), 0);
        Question q2 = publishedQuestion(quiz.getId(), 1);
        UUID opt1 = UUID.randomUUID();
        UUID opt2 = UUID.randomUUID();

        QuizAttempt attempt = new QuizAttempt(enrollment.getId(), quiz.getId(), 1);
        setId(attempt, UUID.randomUUID());
        attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);

        when(quizAttemptRepository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(enrollmentRepository.findByIdWithDetails(enrollment.getId())).thenReturn(Optional.of(enrollment));
        when(questionRepository.findByQuizIdAndDeletedAtIsNullOrderByOrderIndexAsc(quiz.getId()))
                .thenReturn(List.of(q1, q2));
        when(studentAnswerRepository.findByQuizAttemptId(attempt.getId()))
                .thenReturn(List.of(
                        answer(q1.getId(), opt1, q1Correct),
                        answer(q2.getId(), opt2, q2Correct)));
        when(quizRepository.findById(quiz.getId())).thenReturn(Optional.of(quiz));
        when(moduleRepository.findActiveById(module.getId())).thenReturn(Optional.of(module));
        when(quizAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // options only needed when revealResults=true after submit
        when(questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q1.getId()))
                .thenReturn(List.of(option(q1.getId(), opt1, true), option(q1.getId(), UUID.randomUUID(), false)));
        when(questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q2.getId()))
                .thenReturn(List.of(option(q2.getId(), opt2, true), option(q2.getId(), UUID.randomUUID(), false)));

        QuizAttemptResponse response = attemptService.submit(attempt.getId(), principal);

        assertThat(response.status()).isEqualTo("GRADED");
        assertThat(response.score()).isEqualByComparingTo(new BigDecimal(expectedScore));
        assertThat(response.passed()).isEqualTo(passed);

        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().isMutable()).isFalse();
        assertThat(captor.getValue().getStatus()).isEqualTo(QuizAttemptStatus.GRADED);
    }

    private Question publishedQuestion(UUID quizId, int order) throws Exception {
        Question q = new Question();
        q.setQuizId(quizId);
        q.setStatus(QuestionStatus.PUBLISHED);
        q.setOrderIndex(order);
        q.setStatement("Q" + order);
        setId(q, UUID.randomUUID());
        return q;
    }

    private static QuestionOption option(UUID questionId, UUID id, boolean correct) throws Exception {
        QuestionOption o = new QuestionOption();
        o.setQuestionId(questionId);
        o.setCorrect(correct);
        o.setText(correct ? "certo" : "errado");
        o.setOrderIndex(correct ? 0 : 1);
        setId(o, id);
        return o;
    }

    private StudentAnswer answer(UUID questionId, UUID optionId, boolean correct) throws Exception {
        StudentAnswer a = new StudentAnswer(UUID.randomUUID(), questionId, optionId, correct);
        setId(a, UUID.randomUUID());
        return a;
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
