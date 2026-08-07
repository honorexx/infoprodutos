package com.infoprodutos.api.course;

import com.infoprodutos.api.course.dto.AddInstructorRequest;
import com.infoprodutos.api.course.dto.CourseCreateRequest;
import com.infoprodutos.api.course.dto.CourseResponse;
import com.infoprodutos.api.course.dto.CourseSummaryResponse;
import com.infoprodutos.api.course.dto.CourseUpdateRequest;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de cursos (docs/API.md secao 2.3). Autorização por papel via
 * @PreAuthorize; autorização por posse (dono do curso) reforçada dentro do
 * CourseService via CourseAccessGuard.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<CourseSummaryResponse>> list(
            Pageable pageable, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.list(pageable, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CourseResponse> get(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.get(id, principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public CourseResponse create(
            @Valid @RequestBody CourseCreateRequest request, @AuthenticationPrincipal CustomUserDetails principal) {
        return courseService.create(request, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<CourseResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.update(id, request, principal));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> publish(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.publish(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> unpublish(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.unpublish(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> archive(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.archive(id, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/instructors")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> addInstructor(
            @PathVariable UUID id,
            @Valid @RequestBody AddInstructorRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.addInstructor(id, request.instructorUserId(), request.primary(), principal);
        return ResponseEntity.noContent().build();
    }
}
