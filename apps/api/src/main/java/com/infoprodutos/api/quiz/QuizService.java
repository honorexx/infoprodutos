package com.infoprodutos.api.quiz;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.enrollment.EnrollmentAccessGuard;
import com.infoprodutos.api.quiz.domain.Question;
import com.infoprodutos.api.quiz.domain.QuestionDifficulty;
import com.infoprodutos.api.quiz.domain.QuestionOption;
import com.infoprodutos.api.quiz.domain.QuestionOrigin;
import com.infoprodutos.api.quiz.domain.QuestionStatus;
import com.infoprodutos.api.quiz.domain.Quiz;
import com.infoprodutos.api.quiz.domain.QuizStatus;
import com.infoprodutos.api.quiz.dto.CreateManualQuestionRequest;
import com.infoprodutos.api.quiz.dto.QuestionStaffResponse;
import com.infoprodutos.api.quiz.dto.QuizDetailResponse;
import com.infoprodutos.api.quiz.dto.UpdateManualQuestionRequest;
import com.infoprodutos.api.quiz.repository.QuestionOptionRepository;
import com.infoprodutos.api.quiz.repository.QuestionRepository;
import com.infoprodutos.api.quiz.repository.QuizRepository;
import com.infoprodutos.api.quiz.repository.StudentAnswerRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ModuleRepository moduleRepository;
    private final LessonService lessonService;
    private final CourseAccessGuard courseAccessGuard;
    private final EnrollmentAccessGuard enrollmentAccessGuard;
    private final AuditService auditService;

    @Transactional
    public Quiz getOrCreateQuizForModule(UUID moduleId, CustomUserDetails principal) {
        Module module = moduleRepository
                .findActiveById(moduleId)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
        courseAccessGuard.requireManageAccess(module.getCourse().getId(), principal);

        return quizRepository
                .findByModuleIdAndDeletedAtIsNull(moduleId)
                .orElseGet(() -> {
                    Quiz quiz = new Quiz();
                    quiz.setModuleId(moduleId);
                    quiz.setTitle("Exercício — " + module.getTitle());
                    quiz.setStatus(QuizStatus.DRAFT);
                    return quizRepository.save(quiz);
                });
    }

    @Transactional(readOnly = true)
    public QuizDetailResponse getQuizByModule(UUID moduleId, CustomUserDetails principal) {
        Module module = moduleRepository
                .findActiveById(moduleId)
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
        UUID courseId = module.getCourse().getId();

        boolean staff = courseAccessGuard.canManage(courseId, principal);
        if (!staff && !enrollmentAccessGuard.hasActiveEnrollment(principal.getId(), courseId)) {
            throw new com.infoprodutos.api.common.exception.ForbiddenOperationException(
                    "Sem permissão para acessar este quiz.");
        }

        Quiz quiz = quizRepository
                .findByModuleIdAndDeletedAtIsNull(moduleId)
                .orElseGet(() -> {
                    if (!staff) {
                        throw new NotFoundException("Quiz não encontrado.");
                    }
                    return null;
                });
        if (quiz == null) {
            quiz = new Quiz();
            quiz.setModuleId(moduleId);
            quiz.setTitle("Exercício — " + module.getTitle());
            quiz.setStatus(QuizStatus.DRAFT);
            // não persiste em readOnly — staff verá via getOrCreate no create
            return new QuizDetailResponse(
                    null,
                    moduleId.toString(),
                    quiz.getTitle(),
                    QuizStatus.DRAFT.name(),
                    null,
                    null,
                    0,
                    List.of());
        }

        List<Question> questions = questionRepository.findByQuizIdAndDeletedAtIsNullOrderByOrderIndexAsc(quiz.getId());
        if (!staff) {
            questions = questions.stream().filter(q -> q.getStatus() == QuestionStatus.PUBLISHED).toList();
        }
        List<QuestionStaffResponse> mapped = questions.stream().map(this::toStaffQuestion).toList();
        long published = questions.stream().filter(q -> q.getStatus() == QuestionStatus.PUBLISHED).count();
        return new QuizDetailResponse(
                quiz.getId().toString(),
                quiz.getModuleId().toString(),
                quiz.getTitle(),
                quiz.getStatus().name(),
                quiz.getPassingScore(),
                quiz.getMaxAttempts(),
                (int) published,
                mapped);
    }

    @Transactional
    public QuestionStaffResponse createManualQuestion(
            UUID moduleId, CreateManualQuestionRequest request, CustomUserDetails principal) {
        Quiz quiz = getOrCreateQuizForModule(moduleId, principal);
        Lesson lesson = lessonService.findActiveOrThrow(request.lessonId());
        if (!lesson.getModule().getId().equals(moduleId)) {
            throw new BadRequestException("A aula não pertence a este módulo.");
        }

        List<QuestionOption> draftOptions = mapOptions(null, request.options());
        QuestionStructureRules.requireValidManualOptions(draftOptions);

        Question question = new Question();
        question.setQuizId(quiz.getId());
        question.setLessonId(lesson.getId());
        question.setStatement(request.statement().trim());
        question.setExplanation(request.explanation());
        question.setDifficulty(parseDifficulty(request.difficulty()));
        question.setTopic(request.topic());
        question.setStatus(QuestionStatus.DRAFT);
        question.setOrigin(QuestionOrigin.MANUAL);
        question.setOrderIndex(nextOrderIndex(quiz.getId()));
        question = questionRepository.save(question);

        for (QuestionOption opt : draftOptions) {
            opt.setQuestionId(question.getId());
            questionOptionRepository.save(opt);
        }

        auditService.record(principal.getId(), "QUESTION_MANUAL_CREATED", "Question", question.getId(), null);
        return toStaffQuestion(question);
    }

    @Transactional
    public QuestionStaffResponse updateManualQuestion(
            UUID questionId, UpdateManualQuestionRequest request, CustomUserDetails principal) {
        Question question = questionRepository
                .findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada."));
        requireManageQuestion(question, principal);
        if (question.getOrigin() != QuestionOrigin.MANUAL) {
            throw new BadRequestException("Use o fluxo de revisão de IA para editar questões geradas.");
        }
        if (studentAnswerRepository.existsByQuestionId(questionId)) {
            throw new ConflictException("Questão já possui respostas de alunos e não pode ser reestruturada.");
        }

        List<QuestionOption> existing = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
        questionOptionRepository.deleteAll(existing);

        List<QuestionOption> draftOptions = mapOptions(questionId, request.options());
        QuestionStructureRules.requireValidManualOptions(draftOptions);

        question.setStatement(request.statement().trim());
        question.setExplanation(request.explanation());
        question.setDifficulty(parseDifficulty(request.difficulty()));
        question.setTopic(request.topic());
        if (question.getStatus() == QuestionStatus.PUBLISHED) {
            question.setStatus(QuestionStatus.DRAFT);
            question.setApprovedAt(null);
            question.setApprovedByUserId(null);
        }
        questionRepository.save(question);
        questionOptionRepository.saveAll(draftOptions);

        auditService.record(principal.getId(), "QUESTION_MANUAL_UPDATED", "Question", question.getId(), null);
        return toStaffQuestion(question);
    }

    @Transactional
    public QuestionStaffResponse publishManualQuestion(UUID questionId, CustomUserDetails principal) {
        Question question = questionRepository
                .findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada."));
        requireManageQuestion(question, principal);
        if (question.getOrigin() != QuestionOrigin.MANUAL) {
            throw new BadRequestException("Questões de IA devem passar pelo fluxo de aprovação.");
        }

        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
        QuestionStructureRules.requireValidManualOptions(options);

        question.setStatus(QuestionStatus.PUBLISHED);
        question.setApprovedByUserId(principal.getId());
        question.setApprovedAt(Instant.now());
        questionRepository.save(question);

        ensureQuizPublishedIfHasQuestions(question.getQuizId());
        auditService.record(principal.getId(), "QUESTION_MANUAL_PUBLISHED", "Question", question.getId(), null);
        return toStaffQuestion(question);
    }

    @Transactional
    public void deleteQuestion(UUID questionId, CustomUserDetails principal) {
        Question question = questionRepository
                .findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada."));
        requireManageQuestion(question, principal);
        if (studentAnswerRepository.existsByQuestionId(questionId)) {
            question.setDeletedAt(Instant.now());
            questionRepository.save(question);
        } else {
            List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
            questionOptionRepository.deleteAll(options);
            questionRepository.delete(question);
        }
        auditService.record(principal.getId(), "QUESTION_DELETED", "Question", questionId, null);
    }

    @Transactional
    public QuizDetailResponse publishQuiz(UUID quizId, CustomUserDetails principal) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new NotFoundException("Quiz não encontrado."));
        requireManageQuiz(quiz, principal);
        long published = questionRepository.findByQuizIdAndDeletedAtIsNullOrderByOrderIndexAsc(quizId).stream()
                .filter(q -> q.getStatus() == QuestionStatus.PUBLISHED)
                .count();
        if (published == 0) {
            throw new BadRequestException("Publique ao menos uma questão antes de publicar o quiz.");
        }
        quiz.setStatus(QuizStatus.PUBLISHED);
        quizRepository.save(quiz);
        auditService.record(principal.getId(), "QUIZ_PUBLISHED", "Quiz", quizId, null);
        return getQuizByModule(quiz.getModuleId(), principal);
    }

    Quiz requireQuiz(UUID quizId) {
        return quizRepository.findById(quizId).filter(q -> q.getDeletedAt() == null).orElseThrow(() -> new NotFoundException("Quiz não encontrado."));
    }

    private void ensureQuizPublishedIfHasQuestions(UUID quizId) {
        Quiz quiz = requireQuiz(quizId);
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            quiz.setStatus(QuizStatus.PUBLISHED);
            quizRepository.save(quiz);
        }
    }

    private void requireManageQuestion(Question question, CustomUserDetails principal) {
        Quiz quiz = requireQuiz(question.getQuizId());
        requireManageQuiz(quiz, principal);
    }

    private void requireManageQuiz(Quiz quiz, CustomUserDetails principal) {
        Module module = moduleRepository
                .findActiveById(quiz.getModuleId())
                .orElseThrow(() -> new NotFoundException("Módulo não encontrado."));
        courseAccessGuard.requireManageAccess(module.getCourse().getId(), principal);
    }

    private int nextOrderIndex(UUID quizId) {
        return questionRepository.findByQuizIdAndDeletedAtIsNullOrderByOrderIndexAsc(quizId).stream()
                .mapToInt(Question::getOrderIndex)
                .max()
                .orElse(-1)
                + 1;
    }

    private static QuestionDifficulty parseDifficulty(String raw) {
        try {
            return QuestionDifficulty.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Dificuldade inválida. Use EASY, MEDIUM ou HARD.");
        }
    }

    private static List<QuestionOption> mapOptions(
            UUID questionId, List<CreateManualQuestionRequest.OptionInput> inputs) {
        List<QuestionOption> list = new ArrayList<>();
        for (CreateManualQuestionRequest.OptionInput in : inputs) {
            QuestionOption opt = new QuestionOption();
            opt.setQuestionId(questionId);
            opt.setText(in.text().trim());
            opt.setCorrect(Boolean.TRUE.equals(in.correct()));
            opt.setOrderIndex(in.orderIndex());
            list.add(opt);
        }
        list.sort(Comparator.comparingInt(QuestionOption::getOrderIndex));
        return list;
    }

    private QuestionStaffResponse toStaffQuestion(Question question) {
        List<QuestionOption> options =
                questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());
        return new QuestionStaffResponse(
                question.getId().toString(),
                question.getQuizId().toString(),
                question.getLessonId().toString(),
                question.getStatement(),
                question.getExplanation(),
                question.getDifficulty().name(),
                question.getTopic(),
                question.getStatus().name(),
                question.getOrigin().name(),
                question.getOrderIndex(),
                question.getCreatedAt(),
                options.stream()
                        .map(o -> new QuestionStaffResponse.OptionStaffResponse(
                                o.getId().toString(), o.getText(), o.isCorrect(), o.getOrderIndex()))
                        .toList());
    }
}
