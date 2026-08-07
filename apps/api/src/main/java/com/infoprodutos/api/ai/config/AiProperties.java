package com.infoprodutos.api.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {
    private String provider = "mock";
    private int maxAttempts = 3;
    private int stuckTimeoutMinutes = 15;
    private int defaultQuestionCount = 5;
}
