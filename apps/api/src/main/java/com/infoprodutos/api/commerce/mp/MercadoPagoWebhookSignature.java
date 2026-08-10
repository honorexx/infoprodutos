package com.infoprodutos.api.commerce.mp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Valida o header {@code x-signature} das notificações Webhook do Mercado Pago
 * (HMAC-SHA256 do manifesto {@code id:...;request-id:...;ts:...;}).
 */
public final class MercadoPagoWebhookSignature {

    private MercadoPagoWebhookSignature() {}

    /**
     * @return true se o secret estiver em branco (validação desligada) ou se a assinatura for válida
     */
    public static boolean isValid(String secret, String dataId, String xRequestId, String xSignatureHeader) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (dataId == null || dataId.isBlank() || xRequestId == null || xRequestId.isBlank()
                || xSignatureHeader == null || xSignatureHeader.isBlank()) {
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim().toLowerCase(Locale.ROOT);
            String value = kv[1].trim();
            if ("ts".equals(key)) {
                ts = value;
            } else if ("v1".equals(key)) {
                v1 = value;
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        String expected = hmacSha256Hex(secret, manifest);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256 do webhook MP", e);
        }
    }
}
