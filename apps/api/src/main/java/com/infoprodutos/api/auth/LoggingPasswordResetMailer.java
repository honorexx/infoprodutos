package com.infoprodutos.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão (limitação conhecida da Fase 1): não existe provedor
 * de e-mail configurado ainda, então o link de redefinição é apenas
 * registrado em log estruturado, nunca exposto em resposta HTTP.
 * Ver docs/DECISIONS.md e docs/ROADMAP.md para o plano de substituição por
 * um provedor real (SMTP/SES/SendGrid) antes de produção.
 */
@Component
public class LoggingPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailer.class);

    @Override
    public void sendPasswordResetLink(String toEmail, String resetLink) {
        log.info(
                "[DEV] Envio de e-mail de recuperação de senha não configurado. "
                        + "Link gerado para {}: {} (implementar provedor real antes de produção)",
                toEmail,
                resetLink);
    }
}
