package com.infoprodutos.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.auth.AuthService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.domain.UserStatus;
import com.infoprodutos.api.user.dto.UserSummaryResponse;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public Page<UserSummaryResponse> listUsers(Pageable pageable) {
        return userRepository.findAllActive(pageable).map(UserSummaryResponse::from);
    }

    public UserSummaryResponse getUser(UUID id) {
        return UserSummaryResponse.from(findActiveOrThrow(id));
    }

    @Transactional
    public void block(UUID targetUserId, UUID actorUserId) {
        User user = findActiveOrThrow(targetUserId);
        if (user.getStatus() == UserStatus.BLOCKED) {
            return;
        }
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        authService.revokeAllRefreshTokens(user);
        auditService.record(actorUserId, "USER_BLOCKED", "User", user.getId(), toJson(Map.of("email", user.getEmail())));
    }

    @Transactional
    public void unblock(UUID targetUserId, UUID actorUserId) {
        User user = findActiveOrThrow(targetUserId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditService.record(actorUserId, "USER_UNBLOCKED", "User", user.getId(), toJson(Map.of("email", user.getEmail())));
    }

    @Transactional
    public void assignRole(UUID targetUserId, String roleCode, UUID actorUserId) {
        User user = findActiveOrThrow(targetUserId);
        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new BadRequestException("Papel inválido: " + roleCode));
        if (user.getRoles().contains(role)) {
            return;
        }
        user.getRoles().add(role);
        userRepository.save(user);
        auditService.record(
                actorUserId, "USER_ROLE_ASSIGNED", "User", user.getId(), toJson(Map.of("role", roleCode)));
    }

    private User findActiveOrThrow(UUID id) {
        return userRepository
                .findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
