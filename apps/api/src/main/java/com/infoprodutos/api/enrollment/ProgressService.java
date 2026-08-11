package com.infoprodutos.api.enrollment;

import com.infoprodutos.api.certificate.CertificateEligibilityService;
import com.infoprodutos.api.certificate.repository.CertificateRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.LessonStatus;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.domain.ModuleStatus;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.course.repository.ModuleRepository;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.domain.LessonProgress;
import com.infoprodutos.api.enrollment.domain.LessonProgressStatus;
import com.infoprodutos.api.enrollment.dto.LessonProgressResponse;
import com.infoprodutos.api.enrollment.dto.ProgressHeartbeatRequest;
import com.infoprodutos.api.course.CourseCoverUrls;
import com.infoprodutos.api.enrollment.dto.ProgressSummaryResponse;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.enrollment.repository.LessonProgressRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final String SEQUENTIAL_LOCK_MESSAGE = "Conclua a aula anterior antes de avançar.";

    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonService lessonService;
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final CourseAccessGuard courseAccessGuard;
    private final CertificateEligibilityService certificateEligibilityService;
    private final CertificateRepository certificateRepository;

    @Transactional
    public LessonProgressResponse start(UUID enrollmentId, UUID lessonId, CustomUserDetails principal) {
        Enrollment enrollment = requireActiveOwnedEnrollment(enrollmentId, principal);
        Lesson lesson = requireLessonInCourse(lessonId, enrollment.getCourse().getId());
        requirePriorLessonsCompleted(enrollment, lesson);

        LessonProgress progress = getOrCreate(enrollment, lesson);
        if (progress.getStatus() == LessonProgressStatus.NOT_STARTED) {
            progress.setStatus(LessonProgressStatus.IN_PROGRESS);
            progress.setStartedAt(Instant.now());
            progress = lessonProgressRepository.save(progress);
        }
        return LessonProgressResponse.from(progress);
    }

    @Transactional
    public LessonProgressResponse heartbeat(
            UUID enrollmentId, UUID lessonId, ProgressHeartbeatRequest request, CustomUserDetails principal) {
        Enrollment enrollment = requireActiveOwnedEnrollment(enrollmentId, principal);
        Lesson lesson = requireLessonInCourse(lessonId, enrollment.getCourse().getId());
        requirePriorLessonsCompleted(enrollment, lesson);

        LessonProgress progress = getOrCreate(enrollment, lesson);
        if (progress.getStatus() == LessonProgressStatus.NOT_STARTED) {
            progress.setStatus(LessonProgressStatus.IN_PROGRESS);
            progress.setStartedAt(Instant.now());
        }

        int position = request.positionSeconds();
        // Mantém o máximo para retomada e para o limiar de conclusão.
        if (position > progress.getLastPositionSeconds()) {
            progress.setLastPositionSeconds(position);
        }

        if (progress.getStatus() != LessonProgressStatus.COMPLETED
                && LessonCompletionRules.reachesWatchThreshold(
                        progress.getLastPositionSeconds(), resolveDuration(lesson))) {
            markCompleted(progress);
        }

        progress = lessonProgressRepository.save(progress);
        return LessonProgressResponse.from(progress);
    }

    @Transactional
    public LessonProgressResponse complete(UUID enrollmentId, UUID lessonId, CustomUserDetails principal) {
        Enrollment enrollment = requireActiveOwnedEnrollment(enrollmentId, principal);
        Lesson lesson = requireLessonInCourse(lessonId, enrollment.getCourse().getId());
        requirePriorLessonsCompleted(enrollment, lesson);

        LessonProgress progress = getOrCreate(enrollment, lesson);
        if (progress.getStatus() == LessonProgressStatus.NOT_STARTED) {
            progress.setStartedAt(Instant.now());
        }
        if (progress.getStatus() != LessonProgressStatus.COMPLETED) {
            markCompleted(progress);
            progress = lessonProgressRepository.save(progress);
        }
        return LessonProgressResponse.from(progress);
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse summary(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = enrollmentService.findOrThrow(enrollmentId);
        requireSummaryAccess(enrollment, principal);

        UUID courseId = enrollment.getCourse().getId();
        List<Module> modules = moduleRepository.findAllActiveByCourseOrderByOrderIndex(courseId).stream()
                .filter(m -> m.getStatus() == ModuleStatus.PUBLISHED)
                .toList();

        Map<UUID, LessonProgress> progressByLesson = new HashMap<>();
        for (LessonProgress lp : lessonProgressRepository.findAllByEnrollmentIdWithLesson(enrollmentId)) {
            progressByLesson.put(lp.getLesson().getId(), lp);
        }

        List<ProgressSummaryResponse.ModuleProgressSummary> moduleSummaries = new ArrayList<>();
        int totalLessons = 0;
        int completedLessons = 0;

        for (Module module : modules) {
            List<Lesson> lessons = lessonRepository.findAllActiveByModuleOrderByOrderIndex(module.getId()).stream()
                    .filter(l -> l.getStatus() == LessonStatus.PUBLISHED)
                    .toList();

            List<ProgressSummaryResponse.LessonProgressItem> lessonItems = new ArrayList<>();
            int moduleCompleted = 0;
            for (Lesson lesson : lessons) {
                LessonProgress lp = progressByLesson.get(lesson.getId());
                LessonProgressStatus status =
                        lp != null ? lp.getStatus() : LessonProgressStatus.NOT_STARTED;
                if (status == LessonProgressStatus.COMPLETED) {
                    moduleCompleted++;
                }
                lessonItems.add(new ProgressSummaryResponse.LessonProgressItem(
                        lesson.getId().toString(),
                        lesson.getTitle(),
                        lesson.getOrderIndex(),
                        lesson.getDurationSeconds(),
                        lesson.getAccessType().name(),
                        status.name(),
                        lp != null ? lp.getLastPositionSeconds() : 0,
                        lesson.getCurrentVideoAssetId() != null
                                ? lesson.getCurrentVideoAssetId().toString()
                                : null));
            }

            int moduleTotal = lessons.size();
            totalLessons += moduleTotal;
            completedLessons += moduleCompleted;
            double modulePct = moduleTotal == 0 ? 0.0 : (100.0 * moduleCompleted / moduleTotal);
            moduleSummaries.add(new ProgressSummaryResponse.ModuleProgressSummary(
                    module.getId().toString(),
                    module.getTitle(),
                    module.getOrderIndex(),
                    moduleTotal,
                    moduleCompleted,
                    round1(modulePct),
                    lessonItems));
        }

        double coursePct = totalLessons == 0 ? 0.0 : (100.0 * completedLessons / totalLessons);
        boolean allDone = totalLessons > 0 && completedLessons == totalLessons;
        boolean canFinish = allDone && enrollment.getCompletedAt() == null;
        var certificate = certificateRepository.findByEnrollmentId(enrollmentId);
        boolean canIssue = enrollment.getCompletedAt() != null
                && certificate.isEmpty()
                && certificateEligibilityService.isEligibleForCertificate(enrollment);

        return new ProgressSummaryResponse(
                enrollment.getId().toString(),
                courseId.toString(),
                enrollment.getCourse().getTitle(),
                CourseCoverUrls.resolveForApi(enrollment.getCourse()),
                enrollment.getStatus().name(),
                totalLessons,
                completedLessons,
                round1(coursePct),
                canFinish,
                enrollment.getCompletedAt(),
                canIssue,
                certificate.map(c -> c.getId().toString()).orElse(null),
                moduleSummaries);
    }

    /**
     * Bloqueia avanço para aulas posteriores até as anteriores publicadas estarem COMPLETED.
     * No-op para quem gerencia o curso; no-op sem matrícula ACTIVE (caminho FREE_PREVIEW).
     */
    public void requireSequentialAccess(Lesson lesson, CustomUserDetails principal) {
        UUID courseId = lesson.getModule().getCourse().getId();
        if (courseAccessGuard.canManage(courseId, principal)) {
            return;
        }
        Optional<Enrollment> enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(principal.getId(), courseId)
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE);
        if (enrollment.isEmpty()) {
            return;
        }
        requirePriorLessonsCompleted(enrollment.get(), lesson);
    }

    private void requirePriorLessonsCompleted(Enrollment enrollment, Lesson targetLesson) {
        UUID courseId = enrollment.getCourse().getId();
        UUID targetId = targetLesson.getId();

        List<UUID> priorLessonIds = new ArrayList<>();
        boolean found = false;
        for (Module module : moduleRepository.findAllActiveByCourseOrderByOrderIndex(courseId)) {
            if (module.getStatus() != ModuleStatus.PUBLISHED) {
                continue;
            }
            for (Lesson lesson :
                    lessonRepository.findAllActiveByModuleOrderByOrderIndex(module.getId())) {
                if (lesson.getStatus() != LessonStatus.PUBLISHED) {
                    continue;
                }
                if (lesson.getId().equals(targetId)) {
                    found = true;
                    break;
                }
                priorLessonIds.add(lesson.getId());
            }
            if (found) {
                break;
            }
        }

        if (priorLessonIds.isEmpty()) {
            return;
        }

        Map<UUID, LessonProgressStatus> statusByLesson = new HashMap<>();
        for (LessonProgress lp : lessonProgressRepository.findAllByEnrollmentIdWithLesson(enrollment.getId())) {
            statusByLesson.put(lp.getLesson().getId(), lp.getStatus());
        }
        for (UUID priorId : priorLessonIds) {
            if (statusByLesson.get(priorId) != LessonProgressStatus.COMPLETED) {
                throw new ForbiddenOperationException(SEQUENTIAL_LOCK_MESSAGE);
            }
        }
    }

    private Enrollment requireActiveOwnedEnrollment(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = enrollmentService.findOrThrow(enrollmentId);
        if (!enrollment.getStudent().getId().equals(principal.getId())) {
            throw new ForbiddenOperationException("Você não pode alterar o progresso de outro aluno.");
        }
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ForbiddenOperationException("Matrícula não está ativa.");
        }
        return enrollment;
    }

    private void requireSummaryAccess(Enrollment enrollment, CustomUserDetails principal) {
        if (enrollment.getStudent().getId().equals(principal.getId())) {
            return;
        }
        if (principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)
                || courseAccessGuard.canManage(enrollment.getCourse().getId(), principal)) {
            return;
        }
        throw new ForbiddenOperationException("Sem permissão para ver este progresso.");
    }

    private Lesson requireLessonInCourse(UUID lessonId, UUID courseId) {
        Lesson lesson = lessonService.findActiveOrThrow(lessonId);
        if (!lesson.getModule().getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Aula não pertence ao curso da matrícula.");
        }
        if (lesson.getStatus() != LessonStatus.PUBLISHED) {
            throw new BadRequestException("Aula não está publicada.");
        }
        return lesson;
    }

    private LessonProgress getOrCreate(Enrollment enrollment, Lesson lesson) {
        return lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getId(), lesson.getId())
                .orElseGet(() -> {
                    LessonProgress created = new LessonProgress(enrollment, lesson);
                    return lessonProgressRepository.save(created);
                });
    }

    private static void markCompleted(LessonProgress progress) {
        progress.setStatus(LessonProgressStatus.COMPLETED);
        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(Instant.now());
        }
        if (progress.getStartedAt() == null) {
            progress.setStartedAt(Instant.now());
        }
    }

    private static Integer resolveDuration(Lesson lesson) {
        return lesson.getDurationSeconds();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
