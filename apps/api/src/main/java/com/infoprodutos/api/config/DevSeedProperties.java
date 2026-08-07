package com.infoprodutos.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do seed de dados de desenvolvimento. Só é honrada quando o
 * perfil "dev" está ativo (ver DevDataSeeder) - nunca em produção
 * (docs/SECURITY.md secao 8).
 */
@ConfigurationProperties(prefix = "app.dev-seed")
@Getter
@Setter
public class DevSeedProperties {

    private boolean enabled = false;
    private String adminEmail;
    private String adminPassword;
    private String instructorEmail;
    private String instructorPassword;
    private String studentEmail;
    private String studentPassword;
}
