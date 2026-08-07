package com.infoprodutos.api.ai;

import com.infoprodutos.api.ai.config.AiProperties;
import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.domain.AiJobStatus;
import com.infoprodutos.api.ai.dto.AiJobResponse;
import com.infoprodutos.api.ai.dto.CreateAiJobRequest;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.video.domain.ProcessingStatus;
import com.infoprodutos.api.video.domain.StorageProviderType;
import com.infoprodutos.api.video.domain.UploadStatus;
import com.infoprodutos.api.video.domain.VideoAsset;
import com.infoprodutos.api.video.repository.VideoAssetRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class AiJobService {

    private final AiGenerationJobRepository jobRepository;
    private final LessonService lessonService;
    private final CourseAccessGuard accessGuard;
    private final CourseInstructorRepository courseInstructorRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final LessonRepository lessonRepository;
    private final AiJobWorker aiJobWorker;
    private final AiProperties aiProperties;
    private final AuditService auditService;
    private final Environment environment;

    @Transactional
    public AiJobResponse create(UUID lessonId, CreateAiJobRequest request, CustomUserDetails principal) {
        Lesson lesson = lessonService.findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);

        return jobRepository
                .findByIdempotencyKey(request.idempotencyKey())
                .map(AiJobResponse::from)
                .orElseGet(() -> createNew(lesson, request, principal));
    }

    private AiJobResponse createNew(Lesson lesson, CreateAiJobRequest request, CustomUserDetails principal) {
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev"));
        String devText = request.devTranscriptText();
        if (devText != null && !devText.isBlank() && !isDev) {
            throw new BadRequestException("devTranscriptText só é permitido no perfil de desenvolvimento.");
        }

        UUID videoId = lesson.getCurrentVideoAssetId();
        if (videoId == null && (devText == null || devText.isBlank())) {
            throw new BadRequestException("Associe um vídeo à aula antes de solicitar geração por IA.");
        }

        if (videoId == null) {
            // Stub de vídeo para E2E com texto de desenvolvimento.
            VideoAsset stub = new VideoAsset();
            stub.setLessonId(lesson.getId());
            stub.setStorageProvider(StorageProviderType.LOCAL_DEV);
            stub.setStorageKey("dev-stub/" + lesson.getId());
            stub.setOriginalFilename("dev-transcript.txt");
            stub.setMimeType("text/plain");
            stub.setUploadStatus(UploadStatus.UPLOADED);
            stub.setProcessingStatus(ProcessingStatus.READY);
            stub.setSizeBytes(0L);
            stub = videoAssetRepository.save(stub);
            lesson.setCurrentVideoAssetId(stub.getId());
            lessonRepository.save(lesson);
            videoId = stub.getId();
        }

        int count = request.questionCount() != null
                ? request.questionCount()
                : aiProperties.getDefaultQuestionCount();

        AiGenerationJob job = new AiGenerationJob();
        job.setCourseId(lesson.getModule().getCourse().getId());
        job.setModuleId(lesson.getModule().getId());
        job.setLessonId(lesson.getId());
        job.setVideoAssetId(videoId);
        job.setStatus(AiJobStatus.PENDING);
        job.setRequestedQuestionCount(count);
        job.setLanguage(request.language() != null && !request.language().isBlank()
                ? request.language()
                : "pt-BR");
        job.setExtraInstructions(request.extraInstructions());
        job.setIdempotencyKey(request.idempotencyKey());
        job.setRequestedByUserId(principal.getId());
        job.setDifficultyDistribution(Map.of("EASY", 2, "MEDIUM", 2, "HARD", 1));

        if (devText != null && !devText.isBlank()) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("devTranscriptText", devText.trim());
            job.setUsageMetadata(meta);
        }

        job = jobRepository.save(job);
        auditService.record(principal.getId(), "AI_JOB_CREATED", "AiGenerationJob", job.getId(), null);

        UUID jobId = job.getId();
        // Dispara o worker só após o commit — evita race em que o @Async
        // não encontra o job ainda não commitado.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiJobWorker.processAsync(jobId);
                }
            });
        } else {
            aiJobWorker.processAsync(jobId);
        }
        return AiJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public AiJobResponse get(UUID jobId, CustomUserDetails principal) {
        AiGenerationJob job = findOrThrow(jobId);
        requireJobAccess(job, principal);
        return AiJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<AiJobResponse> list(CustomUserDetails principal) {
        List<AiGenerationJob> jobs;
        if (principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)) {
            jobs = jobRepository.findAllByOrderByCreatedAtDesc();
        } else if (principal.getRoleCodes().contains(RoleCode.INSTRUCTOR)) {
            List<UUID> courseIds = courseInstructorRepository.findCourseIdsByInstructorId(principal.getId());
            jobs = courseIds.isEmpty()
                    ? List.of()
                    : jobRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds);
        } else {
            throw new ForbiddenOperationException("Sem permissão para listar processamentos de IA.");
        }
        return jobs.stream().map(AiJobResponse::from).toList();
    }

    @Transactional
    public AiJobResponse cancel(UUID jobId, CustomUserDetails principal) {
        AiGenerationJob job = findOrThrow(jobId);
        requireJobAccess(job, principal);
        if (job.getStatus() == AiJobStatus.COMPLETED || job.getStatus() == AiJobStatus.FAILED) {
            throw new BadRequestException("Não é possível cancelar um job já finalizado.");
        }
        if (job.getStatus() != AiJobStatus.CANCELLED) {
            job.setStatus(AiJobStatus.CANCELLED);
            jobRepository.save(job);
            auditService.record(principal.getId(), "AI_JOB_CANCELLED", "AiGenerationJob", jobId, null);
        }
        return AiJobResponse.from(job);
    }

    @Transactional
    public AiJobResponse regenerate(UUID jobId, String newIdempotencyKey, CustomUserDetails principal) {
        AiGenerationJob previous = findOrThrow(jobId);
        requireJobAccess(previous, principal);
        CreateAiJobRequest req = new CreateAiJobRequest(
                newIdempotencyKey,
                previous.getRequestedQuestionCount(),
                previous.getLanguage(),
                previous.getExtraInstructions(),
                null);
        return create(previous.getLessonId(), req, principal);
    }

    AiGenerationJob findOrThrow(UUID id) {
        return jobRepository.findById(id).orElseThrow(() -> new NotFoundException("Job de IA não encontrado."));
    }

    void requireJobAccess(AiGenerationJob job, CustomUserDetails principal) {
        accessGuard.requireManageAccess(job.getCourseId(), principal);
    }
}
