package com.infoprodutos.api.commerce;

import com.infoprodutos.api.commerce.mp.MercadoPagoWebhookSignature;
import com.infoprodutos.api.config.MercadoPagoProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final CheckoutService checkoutService;
    private final MercadoPagoProperties mercadoPagoProperties;

    /**
     * Notificações Mercado Pago (query {@code data.id}/{@code type} ou body JSON).
     * Com {@code MP_WEBHOOK_SECRET} configurado, rejeita assinatura inválida com 401.
     * Sem secret (dev local), processa sem validar. Responde 200 após processar o possível.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody(required = false) Map<String, Object> body) {

        String paymentId = dataId;
        String eventType = type != null ? type : topic;

        if ((paymentId == null || paymentId.isBlank()) && body != null) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
                paymentId = String.valueOf(dataMap.get("id"));
            }
            if (body.get("id") != null && paymentId == null) {
                paymentId = String.valueOf(body.get("id"));
            }
            if (eventType == null && body.get("type") != null) {
                eventType = String.valueOf(body.get("type"));
            }
            if (eventType == null && body.get("action") != null) {
                eventType = String.valueOf(body.get("action"));
            }
        }
        if ((paymentId == null || paymentId.isBlank()) && id != null) {
            paymentId = id;
        }

        // Assinatura MP usa data.id da query; fallback no id resolvido do body.
        String signatureDataId = (dataId != null && !dataId.isBlank()) ? dataId : paymentId;
        String secret = mercadoPagoProperties.webhookSecret();
        if (secret != null && !secret.isBlank()) {
            if (!MercadoPagoWebhookSignature.isValid(secret, signatureDataId, xRequestId, xSignature)) {
                log.warn("Webhook MP rejeitado: assinatura inválida ou ausente (data.id={})", signatureDataId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            if (paymentId != null && !paymentId.isBlank()) {
                // merchant_order notifications also arrive; fetchPayment only works for payment ids.
                if (eventType == null
                        || eventType.toLowerCase().contains("payment")
                        || "payment".equalsIgnoreCase(eventType)) {
                    checkoutService.handleMercadoPagoWebhook(paymentId, "payment");
                } else {
                    log.debug("Webhook MP ignorado type={} id={}", eventType, paymentId);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao processar webhook MP paymentId={}: {}", paymentId, e.toString());
        }
        return ResponseEntity.ok().build();
    }
}
