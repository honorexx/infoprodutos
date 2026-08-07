package com.infoprodutos.api.course;

import com.infoprodutos.api.course.dto.LessonRequest;
import com.infoprodutos.api.course.dto.LessonResponse;
import com.infoprodutos.api.course.dto.ReorderRequest;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aulas (docs/API.md secao 2.5). Mesma estrutura de Módulos. Endpoints de
 * vídeo/materiais (§2.5, §2.6) ficam para a Fase 3.
 */
@RestController
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/api/v1/modules/{moduleId}/lessons")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonResponse>> list(
            @PathVariable UUID moduleId, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(lessonService.list(moduleId, principal));
    }

    @PostMapping("/api/v1/modules/{moduleId}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public LessonResponse create(
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return lessonService.create(moduleId, request, principal);
    }

    @PostMapping("/api/v1/modules/{moduleId}/lessons/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ReorderRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        lessonService.reorder(moduleId, request, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/lessons/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<LessonResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(lessonService.update(id, request, principal));
    }

    @DeleteMapping("/api/v1/lessons/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        lessonService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/lessons/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> publish(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        lessonService.publish(id, principal);
        return ResponseEntity.noContent().build();
    }
}
