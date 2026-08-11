package com.infoprodutos.api.commerce.mp;

import com.fasterxml.jackson.databind.JsonNode;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.config.DotEnvFiles;
import com.infoprodutos.api.config.MercadoPagoProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class MercadoPagoClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClientConfig.class);

    @Bean
    public MercadoPagoClient mercadoPagoClient(MercadoPagoProperties properties) {
        MercadoPagoProperties effective = properties;
        if (!properties.isConfigured()) {
            String fromFile = DotEnvFiles.get("MP_ACCESS_TOKEN");
            if (fromFile != null) {
                log.info("Mercado Pago: MP_ACCESS_TOKEN lido de .env (fallback IntelliJ/local)");
                effective = new MercadoPagoProperties(
                        fromFile,
                        properties.webhookSecret(),
                        properties.apiBaseUrl(),
                        properties.mockWhenTokenBlank());
            }
        }
        if (effective.isConfigured()) {
            log.info("Mercado Pago: cliente HTTP ativo (Checkout Pro real)");
            return new HttpClient(effective);
        }
        log.warn("Mercado Pago: MP_ACCESS_TOKEN ausente — usando mock (compra libera na hora)");
        return new MockClient();
    }

    static final class HttpClient implements MercadoPagoClient {
        private final RestClient restClient;

        HttpClient(MercadoPagoProperties properties) {
            this.restClient = RestClient.builder()
                    .baseUrl(properties.apiBaseUrl().replaceAll("/$", ""))
                    .defaultHeader("Authorization", "Bearer " + properties.accessToken().trim())
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();
        }

        @Override
        public PreferenceResult createPreference(PreferenceRequest request) {
            BigDecimal unitPrice = BigDecimal.valueOf(request.amountCents())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            // LinkedHashMap: auto_return só com HTTPS público — localhost + auto_return
            // o MP rejeita com invalid_auto_return.
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("external_reference", request.externalReference());
            body.put(
                    "items",
                    List.of(Map.of(
                            "title",
                            request.title(),
                            "quantity",
                            1,
                            "currency_id",
                            request.currency(),
                            "unit_price",
                            unitPrice)));
            body.put("payer", Map.of("email", request.payerEmail()));
            body.put(
                    "back_urls",
                    Map.of(
                            "success", request.successUrl(),
                            "failure", request.failureUrl(),
                            "pending", request.pendingUrl()));
            if (supportsAutoReturn(request.successUrl())) {
                body.put("auto_return", "approved");
            }
            if (request.notificationUrl() != null && !request.notificationUrl().isBlank()) {
                body.put("notification_url", request.notificationUrl());
            }

            try {
                JsonNode node = restClient
                        .post()
                        .uri("/checkout/preferences")
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                if (node == null || !node.hasNonNull("id")) {
                    throw new BadRequestException("Mercado Pago não retornou preference id.");
                }
                return new PreferenceResult(
                        node.get("id").asText(),
                        textOrNull(node, "init_point"),
                        textOrNull(node, "sandbox_init_point"));
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestException(
                        "Falha ao criar preferência no Mercado Pago: "
                                + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }

        @Override
        public PaymentResult fetchPayment(String paymentId) {
            try {
                JsonNode node = restClient
                        .get()
                        .uri("/v1/payments/{id}", paymentId)
                        .retrieve()
                        .body(JsonNode.class);
                if (node == null) {
                    throw new BadRequestException("Pagamento Mercado Pago não encontrado.");
                }
                return toPaymentResult(node, paymentId);
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                throw new BadRequestException(
                        "Falha ao consultar pagamento no Mercado Pago: "
                                + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }

        @Override
        public List<PaymentResult> searchPaymentsByExternalReference(String externalReference) {
            try {
                JsonNode node = restClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/payments/search")
                                .queryParam("external_reference", externalReference)
                                .queryParam("sort", "date_created")
                                .queryParam("criteria", "desc")
                                .build())
                        .retrieve()
                        .body(JsonNode.class);
                if (node == null || !node.has("results") || !node.get("results").isArray()) {
                    return List.of();
                }
                java.util.ArrayList<PaymentResult> results = new java.util.ArrayList<>();
                for (JsonNode item : node.get("results")) {
                    results.add(toPaymentResult(item, item.path("id").asText("")));
                }
                return results;
            } catch (Exception e) {
                throw new BadRequestException(
                        "Falha ao buscar pagamentos no Mercado Pago: "
                                + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            }
        }

        private static PaymentResult toPaymentResult(JsonNode node, String fallbackId) {
            long cents = 0L;
            if (node.has("transaction_amount") && !node.get("transaction_amount").isNull()) {
                cents = node.get("transaction_amount")
                        .decimalValue()
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();
            }
            String preferenceId = null;
            if (node.has("preference_id") && !node.get("preference_id").isNull()) {
                preferenceId = node.get("preference_id").asText();
            }
            return new PaymentResult(
                    node.path("id").asText(fallbackId),
                    node.path("status").asText(""),
                    textOrNull(node, "external_reference"),
                    preferenceId,
                    cents,
                    textOrNull(node, "currency_id"));
        }

        private static String textOrNull(JsonNode node, String field) {
            return node.hasNonNull(field) ? node.get(field).asText() : null;
        }

        /** MP exige back_urls.success HTTPS (não-localhost) para auto_return. */
        private static boolean supportsAutoReturn(String successUrl) {
            if (successUrl == null || successUrl.isBlank()) {
                return false;
            }
            String lower = successUrl.toLowerCase();
            return lower.startsWith("https://")
                    && !lower.contains("://localhost")
                    && !lower.contains("://127.0.0.1");
        }
    }

    /** Preferências fake; aprovação via {@code POST /checkout/orders/{id}/simulate-payment}. */
    public static final class MockClient implements MercadoPagoClient {
        private final Map<String, PaymentResult> payments = new ConcurrentHashMap<>();

        @Override
        public PreferenceResult createPreference(PreferenceRequest request) {
            String preferenceId = "mock-pref-" + UUID.randomUUID();
            // initPoint aponta de volta ao front; o front chama simulate-payment em mockMode.
            String init = request.successUrl().contains("?")
                    ? request.successUrl() + "&mock=1"
                    : request.successUrl() + "?mock=1";
            return new PreferenceResult(preferenceId, init, init);
        }

        @Override
        public PaymentResult fetchPayment(String paymentId) {
            PaymentResult stored = payments.get(paymentId);
            if (stored != null) {
                return stored;
            }
            return new PaymentResult(paymentId, "pending", null, null, 0L, null);
        }

        @Override
        public List<PaymentResult> searchPaymentsByExternalReference(String externalReference) {
            return payments.values().stream()
                    .filter(p -> externalReference.equals(p.externalReference()))
                    .toList();
        }

        public PaymentResult approve(
                String paymentId, String externalReference, String preferenceId, long amountCents) {
            PaymentResult result =
                    new PaymentResult(paymentId, "approved", externalReference, preferenceId, amountCents, "BRL");
            payments.put(paymentId, result);
            return result;
        }
    }
}
