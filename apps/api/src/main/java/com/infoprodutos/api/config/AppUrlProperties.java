package com.infoprodutos.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend")
@Getter
@Setter
public class AppUrlProperties {

    /** Base URL do frontend, usada para montar o link de redefinição de senha. */
    private String baseUrl = "http://localhost:3000";
}
