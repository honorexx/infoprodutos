package com.infoprodutos.api.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dev/default: não envia e-mail real — só loga. */
public class LoggingAppMailer implements AppMailer {

    private static final Logger log = LoggerFactory.getLogger(LoggingAppMailer.class);

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) {
        log.info(
                "[DEV] E-mail não enviado (RESEND_API_KEY ausente). to={} subject={} text={}",
                to,
                subject,
                textBody);
    }
}
