package com.infoprodutos.api.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Carrega {@code .env} local para o Environment sem sobrescrever env do processo/IDE.
 *
 * <p>Precisa rodar <em>antes</em> do {@code ConfigDataEnvironmentPostProcessor}
 * (ordem {@code HIGHEST_PRECEDENCE + 10}), senão {@code ${MP_ACCESS_TOKEN:}} no
 * application.yml resolve vazio e o checkout fica em mock.
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    /** Antes de carregar application.yml (ConfigData = HIGHEST + 10). */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, String> loaded = DotEnvFiles.load();
        if (loaded.isEmpty()) {
            return;
        }
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : loaded.entrySet()) {
            String existing = environment.getProperty(entry.getKey());
            if (existing != null && !existing.isBlank()) {
                continue;
            }
            values.put(entry.getKey(), entry.getValue());
        }
        if (values.isEmpty()) {
            return;
        }
        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (environment.getPropertySources().contains("systemEnvironment")) {
            environment.getPropertySources().addAfter("systemEnvironment", source);
        } else {
            environment.getPropertySources().addFirst(source);
        }
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
