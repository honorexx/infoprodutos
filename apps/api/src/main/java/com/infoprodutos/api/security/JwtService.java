package com.infoprodutos.api.security;

import com.infoprodutos.api.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Emissão e validação de access tokens JWT assinados com HS256. O segredo
 * vem exclusivamente de variável de ambiente (docs/SECURITY.md secao 2/4).
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32; // 256 bits, mínimo exigido por HS256

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina a variável de ambiente JWT_SECRET "
                            + "com um valor aleatório de pelo menos 32 caracteres antes de iniciar a aplicação.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET é muito curto. É necessário um segredo com pelo menos 32 bytes (256 bits) para HS256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, List<String> roleCodes) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roleCodes)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            return new ParsedToken(userId, email, roles == null ? List.of() : roles);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token inválido ou expirado.", ex);
        }
    }

    public record ParsedToken(UUID userId, String email, List<String> roles) {}

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
