package com.infoprodutos.api.course;

import com.infoprodutos.api.course.dto.ModuleRequest;
import com.infoprodutos.api.course.dto.ModuleResponse;
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
 * Módulos (docs/API.md secao 2.4). Rotas de coleção ficam aninhadas em
 * /courses/{courseId}/modules; rotas por id (edição, exclusão, publicação)
 * usam /modules/{id} pois o id já identifica o curso indiretamente.
 */
@RestController
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping("/api/v1/courses/{courseId}/modules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ModuleResponse>> list(
            @PathVariable UUID courseId, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(moduleService.list(courseId, principal));
    }

    @PostMapping("/api/v1/courses/{courseId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ModuleResponse create(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return moduleService.create(courseId, request, principal);
    }

    @PostMapping("/api/v1/courses/{courseId}/modules/reorder")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        moduleService.reorder(courseId, request, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/modules/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ModuleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(moduleService.update(id, request, principal));
    }

    @DeleteMapping("/api/v1/modules/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        moduleService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/modules/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> publish(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        moduleService.publish(id, principal);
        return ResponseEntity.noContent().build();
    }
}
