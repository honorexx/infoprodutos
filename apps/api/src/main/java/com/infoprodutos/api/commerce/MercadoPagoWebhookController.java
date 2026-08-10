package com.infoprodutos.api.commerce;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final CheckoutService checkoutService;

    /**
     * Notificações Mercado Pago (query {@code data.id}/{@code type} ou body JSON).
     * Sempre responde 200 após processar o que for possível — MP reenvia em caso de erro.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "id", required = false) String id,
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
