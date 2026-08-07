package com.infoprodutos.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api")
@Getter
@Setter
public class ApiUrlProperties {

    /** Base pública da API (usada em URLs assinadas de streaming). */
    private String publicBaseUrl = "http://localhost:8090";
}
