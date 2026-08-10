package com.infoprodutos.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.auth.dto.ChangePasswordRequest;
import com.infoprodutos.api.auth.dto.LoginRequest;
import com.infoprodutos.api.auth.dto.RegisterRequest;
import com.infoprodutos.api.auth.repository.PasswordResetTokenRepository;
import com.infoprodutos.api.auth.repository.RefreshTokenRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.config.AppUrlProperties;
import com.infoprodutos.api.config.JwtProperties;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.security.JwtService;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordResetMailer passwordResetMailer;

    @Mock
    private AuditService auditService;

    @Mock
    private com.infoprodutos.api.notification.NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshTokenTtlDays(7);
        AppUrlProperties appUrlProperties = new AppUrlProperties();

        authService = new AuthService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                jwtProperties,
                appUrlProperties,
                passwordResetMailer,
                auditService,
                notificationService);
    }

    @Test
    void register_createsStudentUserAndIssuesTokens() {
        Role studentRole = new Role(RoleCode.STUDENT, "Aluno");
        RegisterRequest request = new RegisterRequest("Aluno Teste", "aluno@example.com", "SenhaForte123");

        when(userRepository.existsByEmailIgnoreCase("aluno@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.STUDENT)).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("SenhaForte123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        AuthService.IssuedTokensWithUser result = authService.register(request);

        assertThat(result.user().getEmail()).isEqualTo("aluno@example.com");
        assertThat(result.user().getRoles()).containsExactly(studentRole);
        assertThat(result.toAuthResponse().accessToken()).isEqualTo("access-token");
        assertThat(result.toAuthResponse().roles()).containsExactly(RoleCode.STUDENT);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest("Aluno", "existe@example.com", "SenhaForte123");
        when(userRepository.existsByEmailIgnoreCase("existe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void login_invalidCredentials_throwsGenericMessage() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("E-mail ou senha incorretos.");
    }

    @Test
    void login_blockedAccount_alsoThrowsGenericMessage() {
        // Mesmo quando a causa real é conta bloqueada (DisabledException), a mensagem
        // devolvida ao cliente deve ser idêntica à de senha incorreta, para evitar
        // que um atacante distinga contas existentes de inexistentes (docs/SECURITY.md).
        LoginRequest request = new LoginRequest("bloqueado@example.com", "SenhaForte123");
        AuthenticationException disabled = mock(AuthenticationException.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(disabled);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("E-mail ou senha incorretos.");
    }

    @Test
    void login_success_updatesLastLoginAndIssuesTokens() {
        User user = new User("Aluno", "aluno@example.com", "hashed");
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        AuthService.IssuedTokensWithUser result = authService.login(new LoginRequest("aluno@example.com", "senha"));

        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(result.toAuthResponse().accessToken()).isEqualTo("access-token");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        User user = new User("Aluno", "aluno@example.com", "hashed-current");
        when(passwordEncoder.matches("errada", "hashed-current")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(user, new ChangePasswordRequest("errada", "novaSenha123")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changePassword_correctCurrentPassword_updatesHash() {
        User user = new User("Aluno", "aluno@example.com", "hashed-current");
        when(passwordEncoder.matches("correta", "hashed-current")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashed-new");
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(any())).thenReturn(List.of());

        authService.changePassword(user, new ChangePasswordRequest("correta", "novaSenha123"));

        assertThat(user.getPasswordHash()).isEqualTo("hashed-new");
    }

    @Test
    void resetPassword_invalidToken_throwsBadRequest() {
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("token-invalido", "novaSenha123"))
                .isInstanceOf(BadRequestException.class);
    }
}
