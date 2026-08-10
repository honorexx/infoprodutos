package com.infoprodutos.api.commerce.mp;

import java.util.Map;

public interface MercadoPagoClient {

    PreferenceResult createPreference(PreferenceRequest request);

    PaymentResult fetchPayment(String paymentId);

    /** Busca pagamentos pelo external_reference (id do pedido). Útil quando o webhook não chega (localhost). */
    java.util.List<PaymentResult> searchPaymentsByExternalReference(String externalReference);

    record PreferenceRequest(
            String externalReference,
            String title,
            long amountCents,
            String currency,
            String payerEmail,
            String successUrl,
            String failureUrl,
            String pendingUrl,
            String notificationUrl) {}

    record PreferenceResult(String preferenceId, String initPoint, String sandboxInitPoint) {}

    record PaymentResult(
            String paymentId,
            String status,
            String externalReference,
            String preferenceId,
            long transactionAmountCents) {

        public boolean isApproved() {
            return "approved".equalsIgnoreCase(status);
        }

        public boolean isRejected() {
            return "rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status);
        }
    }
}
