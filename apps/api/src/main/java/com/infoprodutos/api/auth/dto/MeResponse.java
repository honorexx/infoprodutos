package com.infoprodutos.api.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id, String name, String email, List<String> roles, Instant lastLoginAt, Instant createdAt) {}
