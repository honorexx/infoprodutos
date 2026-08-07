package com.infoprodutos.api.ai;

import com.infoprodutos.api.ai.domain.AiGeneratedQuestionReview;
import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.domain.AiJobStatus;
import com.infoprodutos.api.ai.domain.ReviewStatus;
import com.infoprodutos.api.ai.domain.Transcript;
import com.infoprodutos.api.ai.domain.TranscriptSegment;
import com.infoprodutos.api.ai.domain.TranscriptStatus;
import com.infoprodutos.api.ai.provider.AiContentValidator;
import com.infoprodutos.api.ai.provider.AiUsageTracker;
import com.infoprodutos.api.ai.provider.QuestionGenerationProvider;
import com.infoprodutos.api.ai.provider.TranscriptionProvider;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.GeneratedQuestion;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.OptionDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationInput;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptionResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.UsageMetrics;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.ValidationResult;
import com.infoprodutos.api.ai.config.AiProperties;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.VideoAssetRef;
import com.infoprodutos.api.ai.repository.AiGeneratedQuestionReviewRepository;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import com.infoprodutos.api.ai.repository.TranscriptRepository;
import com.infoprodutos.api.ai.repository.TranscriptSegmentRepository;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.quiz.domain.Question;
import com.infoprodutos.api.quiz.domain.QuestionDifficulty;
import com.infoprodutos.api.quiz.domain.QuestionOption;
import com.infoprodutos.api.quiz.domain.QuestionOrigin;
import com.infoprodutos.api.quiz.domain.QuestionStatus;
import com.infoprodutos.api.quiz.domain.Quiz;
import com.infoprodutos.api.quiz.domain.QuizStatus;
import com.infoprodutos.api.quiz.repository.QuestionOptionRepository;
import com.infoprodutos.api.quiz.repository.QuestionRepository;
import com.infoprodutos.api.quiz.repository.QuizRepository;
import com.infoprodutos.api.video.domain.VideoAsset;
import com.infoprodutos.api.video.repository.VideoAssetRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobWorker {

    private final AiGenerationJobRepository jobRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final AiGeneratedQuestionReviewRepository reviewRepository;
    private final TranscriptionProvider transcriptionProvider;
    private final QuestionGenerationProvider questionGenerationProvider;
    private final AiContentValidator contentValidator;
    private final AiUsageTracker usageTracker;
    private final TransactionTemplate transactionTemplate;
    private final AiProperties aiProperties;

    @Async("aiExecutor")
    public void processAsync(UUID jobId) {
        try {
            transactionTemplate.executeWithoutResult(status -> runPipeline(jobId));
        } catch (Exception e) {
            log.warn("AI job {} failed: {}", jobId, e.getClass().getSimpleName());
            transactionTemplate.executeWithoutResult(status -> failJob(jobId, "Falha no processamento de IA."));
        }
    }

    private void runPipeline(UUID jobId) {
        AiGenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        // Só processa PENDING (criação, resume ou reclaim). Evita reentrada em FAILED/AWAITING_REVIEW.
        if (job.getStatus() != AiJobStatus.PENDING) {
            return;
        }
        if (job.getAttemptCount() >= aiProperties.getMaxAttempts()) {
            failJob(jobId, "Limite de tentativas do job de IA atingido.");
            return;
        }

        // Retomada sem duplicar: se já há questões DRAFT deste job, volta para revisão.
        long existingQuestions = questionRepository.countByAiGenerationJobIdAndDeletedAtIsNull(jobId);
        if (existingQuestions > 0) {
            job.setStatus(AiJobStatus.AWAITING_REVIEW);
            job.setErrorMessage(null);
            job.setCompletedAt(null);
            jobRepository.save(job);
            log.info("AI job {} resumed with {} existing questions — awaiting review", jobId, existingQuestions);
            return;
        }

        job.setStartedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setErrorMessage(null);
        job.setCompletedAt(null);
        jobRepository.save(job);

        Lesson lesson = lessonRepository
                .findById(job.getLessonId())
                .orElseThrow(() -> new IllegalStateException("Aula do job não encontrada"));

        // --- TRANSCRIPTION ---
        job.setStatus(AiJobStatus.TRANSCRIBING);
        jobRepository.save(job);
        if (isCancelled(jobId)) {
            return;
        }

        Transcript transcript = ensureTranscript(job, lesson);
        job.setTranscriptId(transcript.getId());
        job.setStatus(AiJobStatus.TRANSCRIBED);
        jobRepository.save(job);

        List<TranscriptSegment> segments =
                segmentRepository.findByTranscriptIdOrderBySequenceIndexAsc(transcript.getId());
        List<TranscriptSegmentDto> segmentDtos = segments.stream()
                .map(s -> new TranscriptSegmentDto(
                        s.getSequenceIndex(),
                        s.getStartTimeSeconds(),
                        s.getEndTimeSeconds(),
                        s.getText(),
                        s.getTopic()))
                .toList();

        // --- GENERATION ---
        if (isCancelled(jobId)) {
            return;
        }
        job.setStatus(AiJobStatus.GENERATING);
        jobRepository.save(job);

        QuestionGenerationResult generation = questionGenerationProvider.generate(new QuestionGenerationInput(
                job.getCourseId(),
                job.getModuleId(),
                job.getLessonId(),
                lesson.getTitle(),
                transcript.getFullText(),
                segmentDtos,
                job.getRequestedQuestionCount(),
                job.getLanguage(),
                job.getExtraInstructions()));

        job.setProvider(generation.provider());
        job.setModel(generation.model());
        usageTracker.recordUsage(
                jobId,
                new UsageMetrics(
                        generation.provider(),
                        generation.model(),
                        generation.inputTokensEstimate(),
                        generation.outputTokensEstimate(),
                        "GENERATING"));

        ValidationResult validation = contentValidator.validate(generation, segmentDtos);
        Map<String, Object> meta = job.getUsageMetadata() != null
                ? new HashMap<>(job.getUsageMetadata())
                : new HashMap<>();
        meta.put("discardReasons", validation.discardReasons());
        meta.put("validQuestionCount", validation.validQuestions().size());
        job.setUsageMetadata(meta);

        if (!validation.batchAccepted()) {
            failJob(jobId, "Nenhuma questão válida gerada. Ajuste a aula ou tente novamente.");
            return;
        }

        Quiz quiz = quizRepository
                .findByModuleIdAndDeletedAtIsNull(job.getModuleId())
                .orElseGet(() -> {
                    Quiz q = new Quiz();
                    q.setModuleId(job.getModuleId());
                    q.setTitle("Exercícios do módulo");
                    q.setStatus(QuizStatus.DRAFT);
                    return quizRepository.save(q);
                });

        int order = (int) questionRepository.countByAiGenerationJobIdAndDeletedAtIsNull(jobId);
        for (GeneratedQuestion gq : validation.validQuestions()) {
            persistQuestion(job, quiz, lesson, segments, gq, order++);
        }

        job.setStatus(AiJobStatus.AWAITING_REVIEW);
        jobRepository.save(job);
        log.info(
                "AI job {} awaiting review with {} questions",
                jobId,
                validation.validQuestions().size());
    }

    private Transcript ensureTranscript(AiGenerationJob job, Lesson lesson) {
        UUID videoId = job.getVideoAssetId();
        String override = null;
        if (job.getUsageMetadata() != null && job.getUsageMetadata().get("devTranscriptText") instanceof String s) {
            override = s;
        }

        if (videoId != null) {
            var existing = transcriptRepository.findByVideoAssetIdAndStatus(videoId, TranscriptStatus.COMPLETED);
            if (existing.isPresent() && override == null) {
                usageTracker.recordUsage(
                        job.getId(), new UsageMetrics("reuse", "n/a", 0, 0, "TRANSCRIBING"));
                return existing.get();
            }
        }

        VideoAsset video = videoId != null ? videoAssetRepository.findById(videoId).orElse(null) : null;
        VideoAssetRef ref = new VideoAssetRef(
                videoId,
                lesson.getId(),
                lesson.getTitle(),
                video != null ? video.getStorageKey() : null,
                override);

        TranscriptionResult result = transcriptionProvider.transcribe(ref, job.getLanguage());
        usageTracker.recordUsage(
                job.getId(),
                new UsageMetrics(result.provider(), "n/a", result.fullText().length() / 4, 0, "TRANSCRIBING"));

        // Se já existe transcript FAILED/PENDING para o vídeo, recria; se COMPLETED e override, cria novo vínculo
        // no job sem sobrescrever o completed (mantemos um por vídeo UNIQUE — então atualizamos o existente).
        Transcript transcript = videoId != null
                ? transcriptRepository.findByVideoAssetId(videoId).orElseGet(Transcript::new)
                : new Transcript();
        if (transcript.getVideoAssetId() == null) {
            // Dev-only path sem vídeo: usa um UUID sintético? Schema exige video_asset_id UNIQUE NOT NULL.
            // Em produção sempre há vídeo; em dev com override sem vídeo, associamos ao video do job se houver.
            if (videoId == null) {
                throw new IllegalStateException("Transcrição exige vídeo associado (exceto override com vídeo).");
            }
            transcript.setVideoAssetId(videoId);
        }
        transcript.setLanguage(result.language());
        transcript.setFullText(result.fullText());
        transcript.setProvider(result.provider());
        transcript.setStatus(TranscriptStatus.COMPLETED);
        transcript.setCompletedAt(Instant.now());
        transcript = transcriptRepository.save(transcript);

        // Replace segments
        List<TranscriptSegment> old =
                segmentRepository.findByTranscriptIdOrderBySequenceIndexAsc(transcript.getId());
        if (!old.isEmpty()) {
            segmentRepository.deleteAll(old);
            segmentRepository.flush();
        }
        for (TranscriptSegmentDto dto : result.segments()) {
            TranscriptSegment seg = new TranscriptSegment();
            seg.setTranscriptId(transcript.getId());
            seg.setSequenceIndex(dto.sequenceIndex());
            seg.setStartTimeSeconds(dto.startTimeSeconds());
            seg.setEndTimeSeconds(dto.endTimeSeconds());
            seg.setText(dto.text());
            seg.setTopic(dto.topic());
            segmentRepository.save(seg);
        }
        return transcript;
    }

    private void persistQuestion(
            AiGenerationJob job,
            Quiz quiz,
            Lesson lesson,
            List<TranscriptSegment> segments,
            GeneratedQuestion gq,
            int orderIndex) {
        UUID segmentId = matchSegment(segments, gq);

        Question question = new Question();
        question.setQuizId(quiz.getId());
        question.setLessonId(lesson.getId());
        question.setTranscriptSegmentId(segmentId);
        question.setStatement(gq.statement().trim());
        question.setExplanation(gq.explanation());
        question.setDifficulty(QuestionDifficulty.valueOf(gq.difficulty().toUpperCase()));
        question.setTopic(gq.topic());
        question.setStatus(QuestionStatus.DRAFT);
        question.setOrigin(QuestionOrigin.AI_GENERATED);
        question.setAiGenerationJobId(job.getId());
        question.setOrderIndex(orderIndex);
        question = questionRepository.save(question);

        int optIdx = 0;
        for (OptionDto opt : gq.options()) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(question.getId());
            option.setText(opt.text().trim());
            option.setCorrect(opt.correct());
            option.setOrderIndex(optIdx++);
            optionRepository.save(option);
        }

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("statement", gq.statement());
        raw.put("options", gq.options().stream()
                .map(o -> Map.of("text", o.text(), "correct", o.correct()))
                .toList());
        raw.put("explanation", gq.explanation());
        raw.put("difficulty", gq.difficulty());
        raw.put("topic", gq.topic());
        raw.put(
                "evidence",
                Map.of(
                        "excerpt",
                        gq.evidence().excerpt(),
                        "startTimeSeconds",
                        gq.evidence().startTimeSeconds(),
                        "endTimeSeconds",
                        gq.evidence().endTimeSeconds()));

        AiGeneratedQuestionReview review = new AiGeneratedQuestionReview();
        review.setAiGenerationJobId(job.getId());
        review.setQuestionId(question.getId());
        review.setRawAiPayload(raw);
        review.setReviewStatus(ReviewStatus.PENDING);
        reviewRepository.save(review);
    }

    private UUID matchSegment(List<TranscriptSegment> segments, GeneratedQuestion gq) {
        String excerpt = gq.evidence().excerpt().toLowerCase();
        for (TranscriptSegment seg : segments) {
            if (seg.getText().toLowerCase().contains(excerpt.substring(0, Math.min(24, excerpt.length())))) {
                return seg.getId();
            }
        }
        return segments.isEmpty() ? null : segments.get(0).getId();
    }

    private boolean isCancelled(UUID jobId) {
        return jobRepository
                .findById(jobId)
                .map(j -> j.getStatus() == AiJobStatus.CANCELLED)
                .orElse(true);
    }

    private void failJob(UUID jobId, String message) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() == AiJobStatus.CANCELLED) {
                return;
            }
            job.setStatus(AiJobStatus.FAILED);
            job.setErrorMessage(sanitize(message));
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        });
    }

    private static String sanitize(String message) {
        if (message == null) {
            return "Erro no processamento.";
        }
        String cleaned = message.replaceAll("(?i)(exception|stack|token|key|secret)=\\S+", "[redacted]");
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }
}
