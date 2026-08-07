package com.infoprodutos.api.quiz;

import com.infoprodutos.api.quiz.dto.QuizAttemptResponse;
import com.infoprodutos.api.quiz.dto.QuizTakeResponse;
import com.infoprodutos.api.quiz.dto.SubmitAnswerRequest;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @GetMapping("/api/v1/quizzes/{quizId}/take")
    @PreAuthorize("isAuthenticated()")
    public QuizTakeResponse takeView(
            @PathVariable UUID quizId, @AuthenticationPrincipal CustomUserDetails principal) {
        return attemptService.getTakeView(quizId, principal);
    }

    @PostMapping("/api/v1/quizzes/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public QuizAttemptResponse start(
            @PathVariable UUID quizId, @AuthenticationPrincipal CustomUserDetails principal) {
        return attemptService.startAttempt(quizId, principal);
    }

    @GetMapping("/api/v1/quizzes/{quizId}/attempts")
    @PreAuthorize("isAuthenticated()")
    public List<QuizAttemptResponse> listMine(
            @PathVariable UUID quizId, @AuthenticationPrincipal CustomUserDetails principal) {
        return attemptService.listMineForQuiz(quizId, principal);
    }

    @PostMapping("/api/v1/quiz-attempts/{id}/answers")
    @PreAuthorize("isAuthenticated()")
    public QuizAttemptResponse answer(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return attemptService.answer(id, request, principal);
    }

    @PostMapping("/api/v1/quiz-attempts/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public QuizAttemptResponse submit(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return attemptService.submit(id, principal);
    }

    @GetMapping("/api/v1/quiz-attempts/{id}")
    @PreAuthorize("isAuthenticated()")
    public QuizAttemptResponse get(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return attemptService.getAttempt(id, principal);
    }
}
