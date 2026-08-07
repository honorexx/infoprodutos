package com.infoprodutos.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.infoprodutos.api.config.JwtProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService buildService(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setAccessTokenTtlMinutes(15);
        properties.setRefreshTokenTtlDays(7);
        JwtService service = new JwtService(properties);
        service.init();
        return service;
    }

    @Test
    void generatesAndParsesTokenWithSameClaims() {
        JwtService service = buildService("01234567890123456789012345678901");
        UUID userId = UUID.randomUUID();

        String token = service.generateAccessToken(userId, "user@example.com", List.of("STUDENT"));
        JwtService.ParsedToken parsed = service.parse(token);

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.email()).isEqualTo("user@example.com");
        assertThat(parsed.roles()).containsExactly("STUDENT");
    }

    @Test
    void rejectsTamperedToken() {
        JwtService service = buildService("01234567890123456789012345678901");
        String token = service.generateAccessToken(UUID.randomUUID(), "user@example.com", List.of("STUDENT"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtService.InvalidTokenException.class);
    }

    @Test
    void failsFastWhenSecretIsMissing() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(null);
        JwtService service = new JwtService(properties);

        assertThatThrownBy(service::init).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsFastWhenSecretIsTooShort() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        JwtService service = new JwtService(properties);

        assertThatThrownBy(service::init).isInstanceOf(IllegalStateException.class);
    }
}
