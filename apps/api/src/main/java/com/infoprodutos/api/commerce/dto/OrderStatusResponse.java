package com.infoprodutos.api.commerce.dto;

import com.infoprodutos.api.commerce.domain.CommerceOrder;
import java.util.List;

public record OrderStatusResponse(
        String orderId,
        String status,
        String kind,
        long amountCents,
        String currency,
        List<String> courseIds) {

    public static OrderStatusResponse from(CommerceOrder order) {
        return new OrderStatusResponse(
                order.getId().toString(),
                order.getStatus().name(),
                order.getKind().name(),
                order.getAmountCents(),
                order.getCurrency(),
                order.getItems().stream().map(i -> i.getCourse().getId().toString()).toList());
    }
}
