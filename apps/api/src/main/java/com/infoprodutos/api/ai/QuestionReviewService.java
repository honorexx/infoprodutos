package com.infoprodutos.api.ai;

import com.infoprodutos.api.ai.domain.AiGeneratedQuestionReview;
import com.infoprodutos.api.ai.domain.AiGenerationJob;
import com.infoprodutos.api.ai.domain.AiJobStatus;
import com.infoprodutos.api.ai.domain.ReviewStatus;
import com.infoprodutos.api.ai.dto.AiReviewResponse;
import com.infoprodutos.api.ai.dto.UpdateQuestionRequest;
import com.infoprodutos.api.ai.repository.AiGeneratedQuestionReviewRepository;
import com.infoprodutos.api.ai.repository.AiGenerationJobRepository;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.quiz.domain.Question;
import com.infoprodutos.api.quiz.domain.QuestionDifficulty;
import com.infoprodutos.api.quiz.domain.QuestionOption;
import com.infoprodutos.api.quiz.domain.QuestionOrigin;
import com.infoprodutos.api.quiz.domain.QuestionStatus;
import com.infoprodutos.api.quiz.repository.QuestionOptionRepository;
import com.infoprodutos.api.quiz.repository.QuestionRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionReviewService {

    private final AiJobService aiJobService;
    private final AiGenerationJobRepository jobRepository;
    private final AiGeneratedQuestionReviewRepository reviewRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AiReviewResponse> listReviews(UUID jobId, CustomUserDetails principal) {
        AiGenerationJob job = aiJobService.findOrThrow(jobId);
        aiJobService.requireJobAccess(job, principal);
        return reviewRepository.findByAiGenerationJobIdOrderByCreatedAtAsc(jobId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AiReviewResponse updateQuestion(
            UUID questionId, UpdateQuestionRequest request, CustomUserDetails principal) {
        Question question = loadManagedQuestion(questionId, principal);
        long correct = request.options().stream().filter(UpdateQuestionRequest.OptionInput::correct).count();
        if (correct != 1) {
            throw new BadRequestException("Informe exatamente uma alternativa correta.");
        }

        question.setStatement(request.statement().trim());
        question.setExplanation(request.explanation());
        question.setDifficulty(QuestionDifficulty.valueOf(request.difficulty().toUpperCase()));
        question.setTopic(request.topic());
        // Edição não altera raw_ai_payload — rastreabilidade obrigatória.
        if (question.getStatus() == QuestionStatus.APPROVED) {
            question.setStatus(QuestionStatus.DRAFT);
            question.setApprovedAt(null);
            question.setApprovedByUserId(null);
            reviewRepository.findByQuestionId(questionId).ifPresent(r -> {
                r.setReviewStatus(ReviewStatus.PENDING);
                r.setReviewedAt(null);
                r.setReviewedByUserId(null);
                reviewRepository.save(r);
            });
        }
        questionRepository.save(question);

        optionRepository.deleteByQuestionId(questionId);
        int idx = 0;
        for (UpdateQuestionRequest.OptionInput opt : request.options()) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setText(opt.text().trim());
            option.setCorrect(opt.correct());
            option.setOrderIndex(idx++);
            optionRepository.save(option);
        }

        auditService.record(principal.getId(), "QUESTION_EDITED", "Question", questionId, null);
        return toResponse(reviewRepository
                .findByQuestionId(questionId)
                .orElseThrow(() -> new NotFoundException("Revisão não encontrada.")));
    }

    @Transactional
    public AiReviewResponse approve(UUID questionId, CustomUserDetails principal) {
        Question question = loadManagedQuestion(questionId, principal);
        if (question.getOrigin() == QuestionOrigin.AI_GENERATED
                && question.getStatus() == QuestionStatus.PUBLISHED
                && question.getApprovedByUserId() == null) {
            throw new BadRequestException("Questão de IA não pode ser publicada sem aprovação.");
        }
        question.setStatus(QuestionStatus.APPROVED);
        question.setApprovedByUserId(principal.getId());
        question.setApprovedAt(Instant.now());
        questionRepository.save(question);

        AiGeneratedQuestionReview review = reviewRepository
                .findByQuestionId(questionId)
                .orElseThrow(() -> new NotFoundException("Revisão não encontrada."));
        review.setReviewStatus(ReviewStatus.APPROVED);
        review.setReviewedByUserId(principal.getId());
        review.setReviewedAt(Instant.now());
        reviewRepository.save(review);

        maybeCompleteJob(question.getAiGenerationJobId());
        auditService.record(principal.getId(), "QUESTION_APPROVED", "Question", questionId, null);
        return toResponse(review);
    }

    @Transactional
    public AiReviewResponse reject(UUID questionId, String notes, CustomUserDetails principal) {
        Question question = loadManagedQuestion(questionId, principal);
        question.setStatus(QuestionStatus.REJECTED);
        question.setApprovedByUserId(null);
        question.setApprovedAt(null);
        questionRepository.save(question);

        AiGeneratedQuestionReview review = reviewRepository
                .findByQuestionId(questionId)
                .orElseThrow(() -> new NotFoundException("Revisão não encontrada."));
        review.setReviewStatus(ReviewStatus.REJECTED);
        review.setReviewNotes(notes);
        review.setReviewedByUserId(principal.getId());
        review.setReviewedAt(Instant.now());
        reviewRepository.save(review);

        maybeCompleteJob(question.getAiGenerationJobId());
        auditService.record(principal.getId(), "QUESTION_REJECTED", "Question", questionId, null);
        return toResponse(review);
    }

    @Transactional
    public AiReviewResponse publish(UUID questionId, CustomUserDetails principal) {
        Question question = loadManagedQuestion(questionId, principal);
        if (question.getOrigin() == QuestionOrigin.AI_GENERATED) {
            if (question.getStatus() != QuestionStatus.APPROVED || question.getApprovedByUserId() == null) {
                throw new BadRequestException(
                        "Questões geradas por IA só podem ser publicadas após aprovação humana.");
            }
        }
        List<QuestionOption> options = optionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
        if (options.size() != 4 || options.stream().filter(QuestionOption::isCorrect).count() != 1) {
            throw new BadRequestException("Publique apenas questões com 4 alternativas e 1 correta.");
        }
        question.setStatus(QuestionStatus.PUBLISHED);
        questionRepository.save(question);
        auditService.record(principal.getId(), "QUESTION_PUBLISHED", "Question", questionId, null);
        return reviewRepository
                .findByQuestionId(questionId)
                .map(this::toResponse)
                .orElseGet(() -> toResponseWithoutReview(question, options));
    }

    @Transactional
    public List<AiReviewResponse> bulkApprove(List<UUID> questionIds, CustomUserDetails principal) {
        List<AiReviewResponse> out = new ArrayList<>();
        for (UUID id : questionIds) {
            out.add(approve(id, principal));
        }
        return out;
    }

    private void maybeCompleteJob(UUID jobId) {
        if (jobId == null) {
            return;
        }
        long total = reviewRepository.countByAiGenerationJobId(jobId);
        long pending = reviewRepository.countByAiGenerationJobIdAndReviewStatus(jobId, ReviewStatus.PENDING);
        if (total > 0 && pending == 0) {
            jobRepository.findById(jobId).ifPresent(job -> {
                if (job.getStatus() == AiJobStatus.AWAITING_REVIEW) {
                    job.setStatus(AiJobStatus.COMPLETED);
                    job.setCompletedAt(Instant.now());
                    jobRepository.save(job);
                }
            });
        }
    }

    private Question loadManagedQuestion(UUID questionId, CustomUserDetails principal) {
        Question question = questionRepository
                .findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new NotFoundException("Questão não encontrada."));
        if (question.getAiGenerationJobId() != null) {
            AiGenerationJob job = aiJobService.findOrThrow(question.getAiGenerationJobId());
            aiJobService.requireJobAccess(job, principal);
        }
        return question;
    }

    private AiReviewResponse toResponse(AiGeneratedQuestionReview review) {
        Question question = questionRepository
                .findByIdAndDeletedAtIsNull(review.getQuestionId())
                .orElseThrow(() -> new NotFoundException("Questão não encontrada."));
        List<QuestionOption> options =
                optionRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());
        Map<String, Object> evidence = new HashMap<>();
        Object rawEvidence = review.getRawAiPayload() != null ? review.getRawAiPayload().get("evidence") : null;
        if (rawEvidence instanceof Map<?, ?> map) {
            map.forEach((k, v) -> evidence.put(String.valueOf(k), v));
        }
        return new AiReviewResponse(
                review.getId().toString(),
                question.getId().toString(),
                review.getAiGenerationJobId().toString(),
                review.getReviewStatus().name(),
                question.getStatement(),
                question.getExplanation(),
                question.getDifficulty().name(),
                question.getTopic(),
                question.getStatus().name(),
                options.stream()
                        .map(o -> new AiReviewResponse.OptionView(
                                o.getId().toString(), o.getText(), o.isCorrect(), o.getOrderIndex()))
                        .toList(),
                evidence,
                review.getRawAiPayload(),
                review.getReviewedAt() != null ? review.getReviewedAt().toString() : null);
    }

    private AiReviewResponse toResponseWithoutReview(Question question, List<QuestionOption> options) {
        return new AiReviewResponse(
                null,
                question.getId().toString(),
                question.getAiGenerationJobId() != null ? question.getAiGenerationJobId().toString() : null,
                null,
                question.getStatement(),
                question.getExplanation(),
                question.getDifficulty().name(),
                question.getTopic(),
                question.getStatus().name(),
                options.stream()
                        .map(o -> new AiReviewResponse.OptionView(
                                o.getId().toString(), o.getText(), o.isCorrect(), o.getOrderIndex()))
                        .toList(),
                Map.of(),
                Map.of(),
                null);
    }
}
