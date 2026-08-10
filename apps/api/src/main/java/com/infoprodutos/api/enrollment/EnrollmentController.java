package com.infoprodutos.api.enrollment;

import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.dto.CreateEnrollmentRequest;
import com.infoprodutos.api.enrollment.dto.EnrollmentResponse;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public Page<EnrollmentResponse> list(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) EnrollmentStatus status,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return enrollmentService.list(courseId, studentId, status, pageable, principal);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<EnrollmentResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return enrollmentService.listMine(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public EnrollmentResponse grant(
            @Valid @RequestBody CreateEnrollmentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return enrollmentService.grant(request, principal);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public EnrollmentResponse suspend(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return enrollmentService.suspend(id, principal);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public EnrollmentResponse cancel(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return enrollmentService.cancel(id, principal);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public EnrollmentResponse reactivate(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return enrollmentService.reactivate(id, principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void remove(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        enrollmentService.remove(id, principal);
    }
}
