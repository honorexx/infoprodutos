package com.infoprodutos.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String resendApiKey, String from) {

    public MailProperties {
        if (from == null || from.isBlank()) {
            from = "PKS Consultoria <onboarding@resend.dev>";
        }
    }

    public boolean resendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }
}
