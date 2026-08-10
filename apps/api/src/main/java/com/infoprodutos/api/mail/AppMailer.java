package com.infoprodutos.api.mail;

/** Envio de e-mail transacional (reset de senha, matrícula, certificado). */
public interface AppMailer {

    void send(String to, String subject, String textBody, String htmlBody);
}
