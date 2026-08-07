package com.infoprodutos.api.enrollment;

import com.infoprodutos.api.enrollment.dto.LessonProgressResponse;
import com.infoprodutos.api.enrollment.dto.ProgressHeartbeatRequest;
import com.infoprodutos.api.enrollment.dto.ProgressSummaryResponse;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments/{enrollmentId}/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/lessons/{lessonId}/start")
    @PreAuthorize("isAuthenticated()")
    public LessonProgressResponse start(
            @PathVariable UUID enrollmentId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.start(enrollmentId, lessonId, principal);
    }

    @PostMapping("/lessons/{lessonId}/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public LessonProgressResponse heartbeat(
            @PathVariable UUID enrollmentId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody ProgressHeartbeatRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.heartbeat(enrollmentId, lessonId, request, principal);
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("isAuthenticated()")
    public LessonProgressResponse complete(
            @PathVariable UUID enrollmentId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.complete(enrollmentId, lessonId, principal);
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ProgressSummaryResponse summary(
            @PathVariable UUID enrollmentId, @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.summary(enrollmentId, principal);
    }
}
