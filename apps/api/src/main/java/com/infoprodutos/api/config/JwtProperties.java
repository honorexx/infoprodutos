package com.infoprodutos.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /** Segredo simétrico usado para assinar tokens (HS256). Nunca tem default em código. */
    private String secret;

    private int accessTokenTtlMinutes = 15;

    private int refreshTokenTtlDays = 7;
}
