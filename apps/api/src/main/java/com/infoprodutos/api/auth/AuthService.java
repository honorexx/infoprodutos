package com.infoprodutos.api.auth;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.auth.domain.PasswordResetToken;
import com.infoprodutos.api.auth.domain.RefreshToken;
import com.infoprodutos.api.auth.dto.AuthResponse;
import com.infoprodutos.api.auth.dto.ChangePasswordRequest;
import com.infoprodutos.api.auth.dto.LoginRequest;
import com.infoprodutos.api.auth.dto.MeResponse;
import com.infoprodutos.api.auth.dto.RegisterRequest;
import com.infoprodutos.api.auth.repository.PasswordResetTokenRepository;
import com.infoprodutos.api.auth.repository.RefreshTokenRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.config.AppUrlProperties;
import com.infoprodutos.api.config.JwtProperties;
import com.infoprodutos.api.notification.NotificationService;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.security.JwtService;
import com.infoprodutos.api.security.TokenHasher;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.RoleRepository;
import com.infoprodutos.api.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int PASSWORD_RESET_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AppUrlProperties appUrlProperties;
    private final PasswordResetMailer passwordResetMailer;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public record IssuedTokens(String accessToken, String rawRefreshToken, Instant refreshTokenExpiresAt) {}

    @Transactional
    public IssuedTokensWithUser register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Este e-mail já está cadastrado.");
        }
        Role studentRole = roleRepository
                .findByCode(RoleCode.STUDENT)
                .orElseThrow(() -> new IllegalStateException("Papel STUDENT não encontrado - verifique o seed de roles."));

        User user = new User(
                request.name().trim(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(studentRole));
        user = userRepository.save(user);
        notificationService.notifyWelcome(user);

        IssuedTokens tokens = issueTokens(user);
        return new IssuedTokensWithUser(tokens, user);
    }

    @Transactional
    public IssuedTokensWithUser login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            User user = principal.getUser();
            boolean wasReturningUser = user.getLastLoginAt() != null;
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            notificationService.notifyWelcomeBackIfDue(user, wasReturningUser);
            IssuedTokens tokens = issueTokens(user);
            return new IssuedTokensWithUser(tokens, user);
        } catch (AuthenticationException ex) {
            // Mensagem sempre genérica, independente da causa real (credencial errada,
            // conta bloqueada, usuário inexistente) para evitar enumeração de contas
            // (docs/SECURITY.md secao 10).
            log.debug("Falha de autenticação para {}: {}", request.email(), ex.getClass().getSimpleName());
            throw new BadCredentialsException("E-mail ou senha incorretos.");
        }
    }

    @Transactional
    public IssuedTokensWithUser refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Sessão inválida.");
        }
        String hash = TokenHasher.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Sessão inválida."));

        if (!existing.isValid(Instant.now())) {
            throw new BadCredentialsException("Sessão expirada. Faça login novamente.");
        }
        User user = existing.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("Sessão inválida.");
        }

        existing.setRevokedAt(Instant.now());
        IssuedTokens tokens = issueTokens(user);
        // encontra o novo token recém-criado para linkar a cadeia de rotação
        refreshTokenRepository
                .findByTokenHash(TokenHasher.hash(tokens.rawRefreshToken()))
                .ifPresent(newToken -> existing.setReplacedByTokenId(newToken.getId()));
        refreshTokenRepository.save(existing);

        return new IssuedTokensWithUser(tokens, user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = TokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Senha atual incorreta.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        revokeAllRefreshTokens(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findActiveByEmailIgnoreCase(email);
        // Resposta ao chamador é sempre genérica (ver AuthController) independente
        // de o e-mail existir ou não, para não permitir enumeração de contas.
        if (userOpt.isEmpty()) {
            log.debug("Solicitação de recuperação de senha para e-mail não cadastrado: {}", email);
            return;
        }
        User user = userOpt.get();
        String rawToken = TokenHasher.generateRawToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(TokenHasher.hash(rawToken));
        resetToken.setExpiresAt(Instant.now().plus(PASSWORD_RESET_TTL_MINUTES, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);

        String resetLink = appUrlProperties.getBaseUrl() + "/reset-password/" + rawToken;
        passwordResetMailer.sendPasswordResetLink(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenHasher.hash(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Token de redefinição inválido ou expirado."));

        if (!resetToken.isValid(Instant.now())) {
            throw new BadRequestException("Token de redefinição inválido ou expirado.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        revokeAllRefreshTokens(user);
    }

    public MeResponse toMeResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), roles, user.getLastLoginAt(), user.getCreatedAt());
    }

    private IssuedTokens issueTokens(User user) {
        List<String> roleCodes = user.getRoles().stream().map(Role::getCode).toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleCodes);

        String rawRefreshToken = TokenHasher.generateRawToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenTtlDays(), ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHasher.hash(rawRefreshToken));
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new IssuedTokens(accessToken, rawRefreshToken, expiresAt);
    }

    @Transactional
    public void revokeAllRefreshTokens(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        Instant now = Instant.now();
        tokens.forEach(t -> t.setRevokedAt(now));
        refreshTokenRepository.saveAll(tokens);
    }

    public record IssuedTokensWithUser(IssuedTokens tokens, User user) {

        public AuthResponse toAuthResponse() {
            List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
            return new AuthResponse(tokens.accessToken(), user.getId(), user.getName(), user.getEmail(), roles);
        }
    }
}
