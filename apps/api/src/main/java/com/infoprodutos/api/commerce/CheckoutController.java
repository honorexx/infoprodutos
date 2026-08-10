package com.infoprodutos.api.commerce;

import com.infoprodutos.api.commerce.dto.CheckoutSessionRequest;
import com.infoprodutos.api.commerce.dto.CheckoutSessionResponse;
import com.infoprodutos.api.commerce.dto.OrderStatusResponse;
import com.infoprodutos.api.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public CheckoutSessionResponse createSession(
            @Valid @RequestBody CheckoutSessionRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return checkoutService.createSession(request, principal);
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public OrderStatusResponse getOrder(
            @PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        return checkoutService.getOrder(orderId, principal);
    }

    @PostMapping("/orders/{orderId}/sync")
    @PreAuthorize("isAuthenticated()")
    public OrderStatusResponse syncOrder(
            @PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        return checkoutService.syncFromMercadoPago(orderId, principal);
    }

    @PostMapping("/orders/sync-pending")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<OrderStatusResponse> syncPending(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return checkoutService.syncPendingOrders(principal);
    }

    @PostMapping("/orders/{orderId}/simulate-payment")
    @PreAuthorize("isAuthenticated()")
    public OrderStatusResponse simulatePayment(
            @PathVariable UUID orderId, @AuthenticationPrincipal CustomUserDetails principal) {
        return checkoutService.simulatePayment(orderId, principal);
    }
}
