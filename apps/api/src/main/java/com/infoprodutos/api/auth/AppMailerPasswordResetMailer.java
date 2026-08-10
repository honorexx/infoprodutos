package com.infoprodutos.api.auth;

import com.infoprodutos.api.mail.AppMailer;
import org.springframework.stereotype.Component;

/** Recuperação de senha via {@link AppMailer} (Resend em prod / log em dev). */
@Component
public class AppMailerPasswordResetMailer implements PasswordResetMailer {

    private final AppMailer appMailer;

    public AppMailerPasswordResetMailer(AppMailer appMailer) {
        this.appMailer = appMailer;
    }

    @Override
    public void sendPasswordResetLink(String toEmail, String resetLink) {
        String subject = "Redefinição de senha — PKS Consultoria";
        String text = "Recebemos um pedido para redefinir sua senha.\n\n"
                + "Abra o link abaixo (válido por tempo limitado):\n"
                + resetLink
                + "\n\nSe você não pediu isso, ignore este e-mail.";
        String html = "<p>Recebemos um pedido para redefinir sua senha.</p>"
                + "<p><a href=\"" + resetLink + "\">Redefinir senha</a></p>"
                + "<p>Se você não pediu isso, ignore este e-mail.</p>";
        appMailer.send(toEmail, subject, text, html);
    }
}
