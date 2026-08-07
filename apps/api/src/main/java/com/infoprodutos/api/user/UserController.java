package com.infoprodutos.api.user;

import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.dto.AssignRoleRequest;
import com.infoprodutos.api.user.dto.UserSummaryResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos de usuários (docs/API.md secao 2.2). Toda
 * autorização é reforçada aqui via @PreAuthorize - nenhuma regra depende do
 * frontend ocultar botões (docs/PRD.md secao 5).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<UserSummaryResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserSummaryResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> block(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails actor) {
        userService.block(id, actor.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> unblock(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails actor) {
        userService.unblock(id, actor.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> assignRole(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails actor) {
        userService.assignRole(id, request.roleCode(), actor.getId());
        return ResponseEntity.noContent().build();
    }
}
