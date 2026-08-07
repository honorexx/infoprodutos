package com.infoprodutos.api.auth.dto;

import java.util.List;
import java.util.UUID;

/**
 * O refresh token nunca aparece neste corpo de resposta - é enviado apenas
 * via cookie httpOnly (docs/SECURITY.md secao 2).
 */
public record AuthResponse(String accessToken, UUID userId, String name, String email, List<String> roles) {}
