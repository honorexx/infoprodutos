package com.infoprodutos.api.auth;

/**
 * Abstração de envio do e-mail de recuperação de senha.
 * Implementação padrão: {@link AppMailerPasswordResetMailer} via {@code AppMailer}
 * (Resend se {@code RESEND_API_KEY} estiver definido; caso contrário só loga).
 */
public interface PasswordResetMailer {

    void sendPasswordResetLink(String toEmail, String resetLink);
}
