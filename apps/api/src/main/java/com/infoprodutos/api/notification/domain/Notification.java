package com.infoprodutos.api.notification.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 60)
    private String type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "link_href", length = 500)
    private String linkHref;

    @Column(name = "read_at")
    private Instant readAt;

    public Notification(UUID userId, String type, String title, String body, String linkHref) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkHref = linkHref;
    }

    public boolean isUnread() {
        return readAt == null;
    }
}
