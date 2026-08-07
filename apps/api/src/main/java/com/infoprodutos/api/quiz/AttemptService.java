package com.infoprodutos.api.quiz;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
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
import com.infoprodutos.api.quiz.dto.QuizTakeResponse;
import com.infoprodutos.api.quiz.dto.SubmitAnswerRequest;
import com.infoprodutos.api.quiz.repository.QuestionOptionRepository;
import com.infoprodutos.api.quiz.repository.QuestionRepository;
import com.infoprodutos.api.quiz.repository.QuizAttemptRepository;
import com.infoprodutos.api.quiz.repository.QuizRepository;
import com.infoprodutos.api.quiz.repository.StudentAnswerRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleRepository moduleRepository;
    private final CourseAccessGuard courseAccessGuard;

    @Transactional(readOnly = true)
    public QuizTakeResponse getTakeView(UUID quizId, CustomUserDetails principal) {
        Quiz quiz = requirePublishedQuiz(quizId);
        Enrollment enrollment = requireActiveEnrollmentForQuiz(quiz, principal.getId());
        int used = (int) quizAttemptRepository.countByEnrollmentIdAndQuizId(enrollment.getId(), quizId);
        Integer max = resolveMaxAttempts(quiz);
        boolean canStart = max == null || used < max;
        var inProgress = quizAttemptRepository
                .findFirstByEnrollmentIdAndQuizIdAndStatusOrderByAttemptNumberDesc(
                        enrollment.getId(), quizId, QuizAttemptStatus.IN_PROGRESS)
                .orElse(null);

        List<Question> questions = publishedQuestions(quizId);
        List<QuizTakeResponse.QuestionTakeItem> items = questions.stream()
                .map(q -> {
                    List<QuestionOption> options =
                            questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
                    return new QuizTakeResponse.QuestionTakeItem(
                            q.getId().toString(),
                            q.getStatement(),
                            q.getOrderIndex(),
                            options.stream()
                                    .map(o -> new QuizTakeResponse.OptionTakeItem(
                                            o.getId().toString(), o.getText(), o.getOrderIndex()))
                                    .toList());
                })
                .toList();

        return new QuizTakeResponse(
                quiz.getId().toString(),
                quiz.getModuleId().toString(),
                quiz.getTitle(),
                max,
                used,
                canStart && inProgress == null,
                inProgress != null ? inProgress.getId().toString() : null,
                items);
    }

    @Transactional
    public QuizAttemptResponse startAttempt(UUID quizId, CustomUserDetails principal) {
        Quiz quiz = requirePublishedQuiz(quizId);
        Enrollment enrollment = requireActiveEnrollmentForQuiz(quiz, principal.getId());

        var existingInProgress = quizAttemptRepository.findFirstByEnrollmentIdAndQuizIdAndStatusOrderByAttemptNumberDesc(
                enrollment.getId(), quizId, QuizAttemptStatus.IN_PROGRESS);
        if (existingInProgress.isPresent()) {
            return toAttemptResponse(existingInProgress.get(), false);
        }

        int used = (int) quizAttemptRepository.countByEnrollmentIdAndQuizId(enrollment.getId(), quizId);
        Integer max = resolveMaxAttempts(quiz);
        if (max != null && used >= max) {
            throw new BadRequestException("Limite de tentativas atingido para este exercício.");
        }
        if (publishedQuestions(quizId).isEmpty()) {
            throw new BadRequestException("Este quiz ainda não possui questões publicadas.");
        }

        int next = quizAttemptRepository.findMaxAttemptNumber(enrollment.getId(), quizId) + 1;
        QuizAttempt attempt = quizAttemptRepository.save(new QuizAttempt(enrollment.getId(), quizId, next));
        return toAttemptResponse(attempt, false);
    }

    @Transactional
    public QuizAttemptResponse answer(UUID attemptId, SubmitAnswerRequest request, CustomUserDetails principal) {
        QuizAttempt attempt = requireOwnedMutableAttempt(attemptId, principal);
        Question question = questionRepository
                .findByIdAndDeletedAtIsNull(request.questionId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada."));
        if (!question.getQuizId().equals(attempt.getQuizId()) || question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new BadRequestException("Questão inválida para esta tentativa.");
        }
        QuestionOption selected = questionOptionRepository
                .findById(request.selectedOptionId())
                .orElseThrow(() -> new NotFoundException("Alternativa não encontrada."));
        if (!selected.getQuestionId().equals(question.getId())) {
            throw new BadRequestException("Alternativa não pertence à questão.");
        }

        StudentAnswer answer = studentAnswerRepository
                .findByQuizAttemptIdAndQuestionId(attemptId, question.getId())
                .orElse(null);
        if (answer == null) {
            answer = new StudentAnswer(attemptId, question.getId(), selected.getId(), selected.isCorrect());
        } else {
            answer.setSelectedOptionId(selected.getId());
            answer.setCorrect(selected.isCorrect());
            answer.setAnsweredAt(Instant.now());
        }
        studentAnswerRepository.save(answer);
        return toAttemptResponse(attempt, false);
    }

    @Transactional
    public QuizAttemptResponse submit(UUID attemptId, CustomUserDetails principal) {
        QuizAttempt attempt = requireOwnedMutableAttempt(attemptId, principal);
        List<Question> questions = publishedQuestions(attempt.getQuizId());
        if (questions.isEmpty()) {
            throw new BadRequestException("Quiz sem questões publicadas.");
        }

        Map<UUID, StudentAnswer> answers = studentAnswerRepository.findByQuizAttemptId(attemptId).stream()
                .collect(Collectors.toMap(StudentAnswer::getQuestionId, Function.identity()));

        int correct = 0;
        for (Question q : questions) {
            StudentAnswer a = answers.get(q.getId());
            if (a != null && a.isCorrect()) {
                correct++;
            }
        }

        BigDecimal score = QuestionStructureRules.scorePercent(correct, questions.size());
        Quiz quiz = quizRepository.findById(attempt.getQuizId()).orElseThrow();
        BigDecimal passing = resolvePassingScore(quiz);

        attempt.setScore(score);
        attempt.setPassed(score.compareTo(passing) >= 0);
        attempt.setStatus(QuizAttemptStatus.GRADED);
        attempt.setSubmittedAt(Instant.now());
        quizAttemptRepository.save(attempt);

        return toAttemptResponse(attempt, true);
    }

    @Transactional(readOnly = true)
    public QuizAttemptResponse getAttempt(UUID attemptId, CustomUserDetails principal) {
        QuizAttempt attempt =
                quizAttemptRepository.findById(attemptId).orElseThrow(() -> new NotFoundException("Tentativa não encontrada."));
        requireViewAttempt(attempt, principal);
        boolean reveal = !attempt.isMutable();
        return toAttemptResponse(attempt, reveal);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptResponse> listMineForQuiz(UUID quizId, CustomUserDetails principal) {
        Quiz quiz = requireQuiz(quizId);
        Enrollment enrollment = requireActiveEnrollmentForQuiz(quiz, principal.getId());
        return quizAttemptRepository.findByEnrollmentIdAndQuizIdOrderByAttemptNumberAsc(enrollment.getId(), quizId)
                .stream()
                .map(a -> toAttemptResponse(a, !a.isMutable()))
                .toList();
    }

    private QuizAttempt requireOwnedMutableAttempt(UUID attemptId, CustomUserDetails principal) {
        QuizAttempt attempt =
                quizAttemptRepository.findById(attemptId).orElseThrow(() -> new NotFoundException("Tentativa não encontrada."));
        Enrollment enrollment = enrollmentRepository
                .findByIdWithDetails(attempt.getEnrollmentId())
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada."));
        if (!enrollment.getStudent().getId().equals(principal.getId())) {
            throw new ForbiddenOperationException("Você não pode alterar a tentativa de outro aluno.");
        }
        if (!attempt.isMutable()) {
            throw new BadRequestException("Tentativa já finalizada e não pode ser alterada.");
        }
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ForbiddenOperationException("Matrícula não está ativa.");
        }
        return attempt;
    }

    private void requireViewAttempt(QuizAttempt attempt, CustomUserDetails principal) {
        Enrollment enrollment = enrollmentRepository
                .findByIdWithDetails(attempt.getEnrollmentId())
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada."));
        if (enrollment.getStudent().getId().equals(principal.getId())) {
            return;
        }
        if (principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)
                || courseAccessGuard.canManage(enrollment.getCourse().getId(), principal)) {
            return;
        }
        throw new ForbiddenOperationException("Sem permissão para ver esta tentativa.");
    }

    private Enrollment requireActiveEnrollmentForQuiz(Quiz quiz, UUID studentId) {
        Module module = moduleRepository
                .findActiveById(quiz.getModuleId())
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
        Course course = module.getCourse();
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, course.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Matrícula ativa necessária."));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ForbiddenOperationException("Matrícula ativa necessária.");
        }
        return enrollment;
    }

    private Quiz requirePublishedQuiz(UUID quizId) {
        Quiz quiz = requireQuiz(quizId);
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new BadRequestException("Quiz ainda não está publicado.");
        }
        return quiz;
    }

    private Quiz requireQuiz(UUID quizId) {
        return quizRepository
                .findById(quizId)
                .filter(q -> q.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Quiz não encontrado."));
    }

    private List<Question> publishedQuestions(UUID quizId) {
        return questionRepository.findByQuizIdAndDeletedAtIsNullOrderByOrderIndexAsc(quizId).stream()
                .filter(q -> q.getStatus() == QuestionStatus.PUBLISHED)
                .toList();
    }

    private Integer resolveMaxAttempts(Quiz quiz) {
        if (quiz.getMaxAttempts() != null) {
            return quiz.getMaxAttempts();
        }
        Module module = moduleRepository.findActiveById(quiz.getModuleId()).orElse(null);
        if (module != null) {
            return module.getCourse().getMaxQuizAttempts();
        }
        return null;
    }

    private BigDecimal resolvePassingScore(Quiz quiz) {
        if (quiz.getPassingScore() != null) {
            return quiz.getPassingScore();
        }
        Module module = moduleRepository.findActiveById(quiz.getModuleId()).orElse(null);
        if (module != null) {
            return module.getCourse().getMinPassingScore();
        }
        return new BigDecimal("70.00");
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt, boolean revealResults) {
        List<QuizAttemptResponse.AnswerResultItem> answers = new ArrayList<>();
        if (revealResults) {
            List<Question> questions = publishedQuestions(attempt.getQuizId());
            Map<UUID, StudentAnswer> byQuestion = studentAnswerRepository.findByQuizAttemptId(attempt.getId()).stream()
                    .collect(Collectors.toMap(StudentAnswer::getQuestionId, Function.identity()));
            for (Question q : questions) {
                List<QuestionOption> options =
                        questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
                QuestionOption correctOpt =
                        options.stream().filter(QuestionOption::isCorrect).findFirst().orElse(null);
                StudentAnswer a = byQuestion.get(q.getId());
                String selectedText = null;
                if (a != null) {
                    selectedText = options.stream()
                            .filter(o -> o.getId().equals(a.getSelectedOptionId()))
                            .map(QuestionOption::getText)
                            .findFirst()
                            .orElse(null);
                }
                answers.add(new QuizAttemptResponse.AnswerResultItem(
                        q.getId().toString(),
                        q.getStatement(),
                        a != null ? a.getSelectedOptionId().toString() : null,
                        selectedText,
                        a != null && a.isCorrect(),
                        correctOpt != null ? correctOpt.getId().toString() : null,
                        q.getExplanation()));
            }
        }
        return new QuizAttemptResponse(
                attempt.getId().toString(),
                attempt.getEnrollmentId().toString(),
                attempt.getQuizId().toString(),
                attempt.getAttemptNumber(),
                attempt.getStatus().name(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getScore(),
                attempt.getPassed(),
                answers);
    }
}
