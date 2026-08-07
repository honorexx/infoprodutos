package com.infoprodutos.api.quiz;

import com.infoprodutos.api.ai.QuestionReviewService;
import com.infoprodutos.api.ai.dto.AiReviewResponse;
import com.infoprodutos.api.ai.dto.UpdateQuestionRequest;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionReviewService questionReviewService;

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public AiReviewResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return questionReviewService.updateQuestion(id, request, principal);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public AiReviewResponse approve(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return questionReviewService.approve(id, principal);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public AiReviewResponse reject(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal) {
        String notes = body != null ? body.get("notes") : null;
        return questionReviewService.reject(id, notes, principal);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public AiReviewResponse publish(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return questionReviewService.publish(id, principal);
    }

    @PostMapping("/bulk-approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public List<AiReviewResponse> bulkApprove(
            @RequestBody @NotEmpty List<UUID> questionIds,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return questionReviewService.bulkApprove(questionIds, principal);
    }
}
