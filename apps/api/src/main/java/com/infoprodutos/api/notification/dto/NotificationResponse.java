package com.infoprodutos.api.notification.dto;

import com.infoprodutos.api.notification.domain.Notification;
import java.time.Instant;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String body,
        String linkHref,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId().toString(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getLinkHref(),
                !n.isUnread(),
                n.getCreatedAt());
    }
}
