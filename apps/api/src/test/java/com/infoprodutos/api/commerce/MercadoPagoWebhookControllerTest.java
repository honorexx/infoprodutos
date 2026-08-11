package com.infoprodutos.api.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import com.infoprodutos.api.config.MercadoPagoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MercadoPagoWebhookControllerTest {

    @Test
    void processingFailureReturnsServiceUnavailableSoNotificationCanBeRetried() {
        CheckoutService checkoutService = org.mockito.Mockito.mock(CheckoutService.class);
        doThrow(new IllegalStateException("temporary outage"))
                .when(checkoutService)
                .handleMercadoPagoWebhook("123", "payment");
        var controller = new MercadoPagoWebhookController(
                checkoutService,
                new MercadoPagoProperties("token", "", "https://api.mercadopago.com", false));

        var response = controller.webhook("123", "payment", null, null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
