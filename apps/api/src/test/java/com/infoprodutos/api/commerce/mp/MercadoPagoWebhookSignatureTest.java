package com.infoprodutos.api.commerce.mp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MercadoPagoWebhookSignatureTest {

    private static final String SECRET = "test-secret";
    private static final String DATA_ID = "123456";
    private static final String REQUEST_ID = "abc-req";
    private static final String TS = "1704908010";
    /** HMAC-SHA256 hex of {@code id:123456;request-id:abc-req;ts:1704908010;} with secret test-secret. */
    private static final String EXPECTED_V1 =
            "e60319e37d69e238bd77ba9b0545064aed2b92018b97f079dfaa3e3778d6d776";

    @Test
    void acceptsValidSignature() {
        String header = "ts=" + TS + ",v1=" + EXPECTED_V1;
        assertThat(MercadoPagoWebhookSignature.isValid(SECRET, DATA_ID, REQUEST_ID, header)).isTrue();
    }

    @Test
    void hmacMatchesKnownManifest() {
        String manifest = "id:" + DATA_ID + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";";
        assertThat(MercadoPagoWebhookSignature.hmacSha256Hex(SECRET, manifest)).isEqualTo(EXPECTED_V1);
    }

    @Test
    void blankSecretSkipsValidation() {
        assertThat(MercadoPagoWebhookSignature.isValid("", DATA_ID, REQUEST_ID, null)).isTrue();
        assertThat(MercadoPagoWebhookSignature.isValid("  ", DATA_ID, REQUEST_ID, "ts=1,v1=bad")).isTrue();
        assertThat(MercadoPagoWebhookSignature.isValid(null, DATA_ID, REQUEST_ID, null)).isTrue();
    }

    @Test
    void rejectsMissingOrInvalidSignatureWhenSecretSet() {
        assertThat(MercadoPagoWebhookSignature.isValid(SECRET, DATA_ID, REQUEST_ID, null)).isFalse();
        assertThat(MercadoPagoWebhookSignature.isValid(SECRET, DATA_ID, REQUEST_ID, "")).isFalse();
        assertThat(MercadoPagoWebhookSignature.isValid(
                        SECRET, DATA_ID, REQUEST_ID, "ts=" + TS + ",v1=deadbeef"))
                .isFalse();
        assertThat(MercadoPagoWebhookSignature.isValid(SECRET, null, REQUEST_ID, "ts=" + TS + ",v1=" + EXPECTED_V1))
                .isFalse();
        assertThat(MercadoPagoWebhookSignature.isValid(SECRET, DATA_ID, null, "ts=" + TS + ",v1=" + EXPECTED_V1))
                .isFalse();
    }
}
