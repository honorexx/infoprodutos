package com.infoprodutos.api.user.dto;

import com.infoprodutos.api.user.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String name,
        String email,
        String status,
        List<String> roles,
        Instant createdAt,
        Instant lastLoginAt) {

    public static UserSummaryResponse from(User user) {
        List<String> roles = user.getRoles().stream().map(r -> r.getCode()).sorted().toList();
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStatus().name(),
                roles,
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
