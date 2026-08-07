package com.infoprodutos.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.auth.AuthService;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.domain.UserStatus;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthService authService;

    @Mock
    private AuditService auditService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, authService, auditService, new ObjectMapper());
    }

    @Test
    void block_setsStatusAndRevokesTokensAndAudits() {
        UUID actorId = UUID.randomUUID();
        User user = new User("Aluno", "aluno@example.com", "hash");
        UUID userId = user.getId();
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        userService.block(userId, actorId);

        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(authService).revokeAllRefreshTokens(user);
        verify(auditService).record(eq(actorId), eq("USER_BLOCKED"), any(), any(), any());
    }

    @Test
    void block_alreadyBlocked_isIdempotent() {
        User user = new User("Aluno", "aluno@example.com", "hash");
        user.setStatus(UserStatus.BLOCKED);
        when(userRepository.findById(any())).thenReturn(Optional.of(user));

        userService.block(user.getId(), UUID.randomUUID());

        verify(authService, never()).revokeAllRefreshTokens(any());
    }

    @Test
    void block_userNotFound_throwsNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.block(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void assignRole_addsRoleWithoutRemovingExisting() {
        Role instructorRole = new Role(RoleCode.INSTRUCTOR, "Professor");
        User user = new User("Usuario", "user@example.com", "hash");
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleCode.INSTRUCTOR)).thenReturn(Optional.of(instructorRole));

        userService.assignRole(user.getId(), RoleCode.INSTRUCTOR, UUID.randomUUID());

        assertThat(user.getRoles()).contains(instructorRole);
    }
}
