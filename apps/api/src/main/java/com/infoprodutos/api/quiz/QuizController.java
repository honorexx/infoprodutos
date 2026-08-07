package com.infoprodutos.api.quiz;

import com.infoprodutos.api.quiz.dto.CreateManualQuestionRequest;
import com.infoprodutos.api.quiz.dto.QuestionStaffResponse;
import com.infoprodutos.api.quiz.dto.QuizDetailResponse;
import com.infoprodutos.api.quiz.dto.UpdateManualQuestionRequest;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/api/v1/modules/{moduleId}/quiz")
    @PreAuthorize("isAuthenticated()")
    public QuizDetailResponse getByModule(
            @PathVariable UUID moduleId, @AuthenticationPrincipal CustomUserDetails principal) {
        return quizService.getQuizByModule(moduleId, principal);
    }

    @PostMapping("/api/v1/modules/{moduleId}/quiz/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public QuestionStaffResponse createQuestion(
            @PathVariable UUID moduleId,
            @Valid @RequestBody CreateManualQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return quizService.createManualQuestion(moduleId, request, principal);
    }

    @PutMapping("/api/v1/questions/{id}/manual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public QuestionStaffResponse updateManual(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManualQuestionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return quizService.updateManualQuestion(id, request, principal);
    }

    @PostMapping("/api/v1/questions/{id}/publish-manual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public QuestionStaffResponse publishManual(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return quizService.publishManualQuestion(id, principal);
    }

    @DeleteMapping("/api/v1/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public void delete(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        quizService.deleteQuestion(id, principal);
    }

    @PostMapping("/api/v1/quizzes/{quizId}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public QuizDetailResponse publishQuiz(
            @PathVariable UUID quizId, @AuthenticationPrincipal CustomUserDetails principal) {
        return quizService.publishQuiz(quizId, principal);
    }
}
