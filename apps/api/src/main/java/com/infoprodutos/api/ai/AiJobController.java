package com.infoprodutos.api.ai;

import com.infoprodutos.api.ai.dto.AiJobResponse;
import com.infoprodutos.api.ai.dto.AiReviewResponse;
import com.infoprodutos.api.ai.dto.CreateAiJobRequest;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiJobController {

    private final AiJobService aiJobService;
    private final QuestionReviewService questionReviewService;

    @PostMapping("/api/v1/lessons/{lessonId}/ai-jobs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public ResponseEntity<AiJobResponse> create(
            @PathVariable UUID lessonId,
            @Valid @RequestBody CreateAiJobRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        AiJobResponse response = aiJobService.create(lessonId, request, principal);
        // Idempotência: job já existente também retorna 200; novo retorna 202.
        HttpStatus status = response.attemptCount() == 0 && "PENDING".equals(response.status())
                ? HttpStatus.ACCEPTED
                : HttpStatus.OK;
        // Sempre 202 na criação nova; se veio do cache de idempotência, 200.
        boolean isNew = response.startedAt() == null && "PENDING".equals(response.status());
        return ResponseEntity.status(isNew ? HttpStatus.ACCEPTED : HttpStatus.OK).body(response);
    }

    @GetMapping("/api/v1/ai-jobs/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public AiJobResponse get(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return aiJobService.get(id, principal);
    }

    @GetMapping("/api/v1/ai-jobs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public List<AiJobResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return aiJobService.list(principal);
    }

    @PostMapping("/api/v1/ai-jobs/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public AiJobResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return aiJobService.cancel(id, principal);
    }

    @PostMapping("/api/v1/ai-jobs/{id}/regenerate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public ResponseEntity<AiJobResponse> regenerate(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal) {
        String key = body.get("idempotencyKey");
        if (key == null || key.isBlank()) {
            key = UUID.randomUUID().toString();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(aiJobService.regenerate(id, key, principal));
    }

    @GetMapping("/api/v1/ai-jobs/{id}/reviews")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public List<AiReviewResponse> reviews(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return questionReviewService.listReviews(id, principal);
    }
}
