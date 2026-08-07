package com.infoprodutos.api.auth;

/**
 * Abstração de envio do e-mail de recuperação de senha. Nenhum provedor de
 * e-mail foi definido ainda para o MVP (docs/DECISIONS.md) - a interface
 * permite plugar um provedor real (SMTP, SES, SendGrid, etc.) sem alterar
 * o AuthService. A implementação padrão apenas registra em log (ambiente de
 * desenvolvimento), nunca envia e-mail real.
 */
public interface PasswordResetMailer {

    void sendPasswordResetLink(String toEmail, String resetLink);
}
