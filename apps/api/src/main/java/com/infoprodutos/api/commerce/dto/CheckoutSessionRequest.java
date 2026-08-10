package com.infoprodutos.api.commerce.dto;

import java.util.UUID;

public record CheckoutSessionRequest(UUID courseId, UUID packageId) {}
