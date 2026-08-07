package com.infoprodutos.api.auth;

import com.infoprodutos.api.auth.dto.AuthResponse;
import com.infoprodutos.api.auth.dto.ChangePasswordRequest;
import com.infoprodutos.api.auth.dto.ForgotPasswordRequest;
import com.infoprodutos.api.auth.dto.LoginRequest;
import com.infoprodutos.api.auth.dto.MeResponse;
import com.infoprodutos.api.auth.dto.RegisterRequest;
import com.infoprodutos.api.auth.dto.ResetPasswordRequest;
import com.infoprodutos.api.config.CookieProperties;
import com.infoprodutos.api.config.JwtProperties;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.IssuedTokensWithUser result = authService.register(request);
        return withRefreshCookie(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.IssuedTokensWithUser result = authService.login(request);
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = extractRefreshCookie(request);
        AuthService.IssuedTokensWithUser result = authService.refresh(rawRefreshToken);
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String rawRefreshToken = extractRefreshCookie(request);
        authService.logout(rawRefreshToken);
        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie.toString()).build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(authService.toMeResponse(principal.getUser()));
    }

    @PostMapping("/password/change")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUser(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        // Resposta sempre genérica, independente de o e-mail existir ou não
        // (docs/SECURITY.md secao 10 - mitigação de enumeração de contas).
        return ResponseEntity.ok(Map.of(
                "message", "Se o e-mail informado estiver cadastrado, um link de redefinição foi enviado."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthService.IssuedTokensWithUser result) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, result.tokens().rawRefreshToken())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(java.time.Duration.ofDays(jwtProperties.getRefreshTokenTtlDays()))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.toAuthResponse());
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
