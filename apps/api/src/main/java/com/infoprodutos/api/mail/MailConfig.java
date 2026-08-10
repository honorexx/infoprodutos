package com.infoprodutos.api.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    AppMailer appMailer(MailProperties properties, ObjectMapper objectMapper) {
        if (properties.resendConfigured()) {
            log.info("Mail: Resend ativo (from={})", properties.from());
            return new ResendAppMailer(properties, objectMapper);
        }
        log.info("Mail: LoggingAppMailer (defina RESEND_API_KEY para envio real)");
        return new LoggingAppMailer();
    }
}
