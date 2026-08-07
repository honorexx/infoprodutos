package com.infoprodutos.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cookie")
@Getter
@Setter
public class CookieProperties {

    /** Deve ser true em produção (HTTPS). Default false para permitir dev local em HTTP. */
    private boolean secure = false;
}
