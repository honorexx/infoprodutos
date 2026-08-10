package com.infoprodutos.api.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.config.MailProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Envia via API Resend (https://api.resend.com/emails). */
public class ResendAppMailer implements AppMailer {

    private static final Logger log = LoggerFactory.getLogger(ResendAppMailer.class);
    private static final URI RESEND_EMAILS = URI.create("https://api.resend.com/emails");

    private final MailProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendAppMailer(MailProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", properties.from());
            body.put("to", List.of(to));
            body.put("subject", subject);
            if (textBody != null && !textBody.isBlank()) {
                body.put("text", textBody);
            }
            if (htmlBody != null && !htmlBody.isBlank()) {
                body.put("html", htmlBody);
            }
            byte[] json = objectMapper.writeValueAsBytes(body);
            HttpRequest request = HttpRequest.newBuilder(RESEND_EMAILS)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + properties.resendApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "Resend falhou status={} to={} subject={} body={}",
                        response.statusCode(),
                        to,
                        subject,
                        response.body());
            } else {
                log.info("E-mail enviado via Resend to={} subject={}", to, subject);
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail via Resend to={} subject={}: {}", to, subject, e.toString());
        }
    }
}
