package com.infoprodutos.api.certificate;

import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.domain.ModuleStatus;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.domain.LessonProgressStatus;
import com.infoprodutos.api.enrollment.repository.LessonProgressRepository;
import com.infoprodutos.api.quiz.domain.Quiz;
import com.infoprodutos.api.quiz.domain.QuizAttemptStatus;
import com.infoprodutos.api.quiz.domain.QuizStatus;
import com.infoprodutos.api.quiz.repository.QuizAttemptRepository;
import com.infoprodutos.api.quiz.repository.QuizRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CertificateEligibilityService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public boolean allPublishedLessonsCompleted(Enrollment enrollment) {
        CourseStats stats = computeLessonStats(enrollment);
        return stats.totalPublished() > 0 && stats.completed() == stats.totalPublished();
    }

    public double completionPercent(Enrollment enrollment) {
        CourseStats stats = computeLessonStats(enrollment);
        if (stats.totalPublished() == 0) {
            return 0.0;
        }
        return 100.0 * stats.completed() / stats.totalPublished();
    }

    /** Valida elegibilidade para emitir certificado; lança BadRequestException se não elegível. */
    public void requireEligibleForCertificate(Enrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Matrícula precisa estar ativa para emitir certificado.");
        }
        Course course = enrollment.getCourse();
        if (!course.isCertificateEnabled()) {
            throw new BadRequestException("Este curso não emite certificado.");
        }
        if (course.getWorkloadHours() == null || course.getWorkloadHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "O curso precisa ter carga horária definida para emitir certificado.");
        }
        if (enrollment.getCompletedAt() == null) {
            throw new BadRequestException("Conclua o curso formalmente antes de emitir o certificado.");
        }

        double pct = completionPercent(enrollment);
        BigDecimal required = course.getMinCompletionPercentage() != null
                ? course.getMinCompletionPercentage()
                : new BigDecimal("100");
        if (BigDecimal.valueOf(pct).compareTo(required) < 0) {
            throw new BadRequestException(
                    "Progresso insuficiente para o certificado (mínimo " + required + "%).");
        }

        requireQuizzesPassed(enrollment, course);
    }

    public boolean isEligibleForCertificate(Enrollment enrollment) {
        try {
            requireEligibleForCertificate(enrollment);
            return true;
        } catch (BadRequestException e) {
            return false;
        }
    }

    private void requireQuizzesPassed(Enrollment enrollment, Course course) {
        BigDecimal minScore = course.getMinPassingScore() != null
                ? course.getMinPassingScore()
                : new BigDecimal("70");

        List<Module> modules = moduleRepository.findAllActiveByCourseOrderByOrderIndex(course.getId()).stream()
                .filter(m -> m.getStatus() == ModuleStatus.PUBLISHED)
                .toList();

        for (Module module : modules) {
            Quiz quiz = quizRepository.findByModuleIdAndDeletedAtIsNull(module.getId()).orElse(null);
            if (quiz == null || quiz.getStatus() != QuizStatus.PUBLISHED) {
                continue;
            }
            BigDecimal best = quizAttemptRepository.findBestGradedScore(enrollment.getId(), quiz.getId());
            if (best == null) {
                throw new BadRequestException(
                        "É necessário concluir o exercício do módulo \"" + module.getTitle() + "\".");
            }
            BigDecimal passing = quiz.getPassingScore() != null ? quiz.getPassingScore() : minScore;
            if (best.compareTo(passing) < 0) {
                throw new BadRequestException(
                        "Nota insuficiente no exercício do módulo \"" + module.getTitle() + "\" (mínimo "
                                + passing + "%).");
            }
        }
    }

    private CourseStats computeLessonStats(Enrollment enrollment) {
        UUID courseId = enrollment.getCourse().getId();
        List<Module> modules = moduleRepository.findAllActiveByCourseOrderByOrderIndex(courseId).stream()
                .filter(m -> m.getStatus() == ModuleStatus.PUBLISHED)
                .toList();

        int total = 0;
        int completed = 0;
        var progress = lessonProgressRepository.findAllByEnrollmentIdWithLesson(enrollment.getId());
        for (Module module : modules) {
            List<Lesson> lessons = lessonRepository.findAllActiveByModuleOrderByOrderIndex(module.getId()).stream()
                    .filter(l -> l.getStatus() == LessonStatus.PUBLISHED)
                    .toList();
            total += lessons.size();
            for (Lesson lesson : lessons) {
                boolean done = progress.stream()
                        .anyMatch(p -> p.getLesson().getId().equals(lesson.getId())
                                && p.getStatus() == LessonProgressStatus.COMPLETED);
                if (done) {
                    completed++;
                }
            }
        }
        return new CourseStats(total, completed);
    }

    private record CourseStats(int totalPublished, int completed) {}
}
