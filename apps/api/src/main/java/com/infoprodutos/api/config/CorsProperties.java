package com.infoprodutos.api.config;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
@Getter
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:3000");

    public void setAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            this.allowedOrigins = List.of("http://localhost:3000");
            return;
        }
        // YAML ${CORS_ALLOWED_ORIGINS:a,b} vira um único elemento "a,b" — separar.
        this.allowedOrigins = allowedOrigins.stream()
                .flatMap(origin -> Arrays.stream(origin.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
