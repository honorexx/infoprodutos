package com.infoprodutos.api.commerce.dto;

public record CheckoutSessionResponse(
        String orderId, String initPoint, String sandboxInitPoint, boolean mockMode) {}
