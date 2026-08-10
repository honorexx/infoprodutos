package com.infoprodutos.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mercadopago")
public record MercadoPagoProperties(
        String accessToken,
        String webhookSecret,
        String apiBaseUrl,
        boolean mockWhenTokenBlank) {

    public MercadoPagoProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.mercadopago.com";
        }
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    public boolean useMock() {
        return !isConfigured() && mockWhenTokenBlank;
    }
}
