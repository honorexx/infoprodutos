package com.infoprodutos.api.commerce.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commerce_order")
@Getter
@Setter
@NoArgsConstructor
public class CommerceOrder extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_user_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private OrderKind kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private ProductPackage productPackage;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "mp_preference_id", length = 120)
    private String mpPreferenceId;

    @Column(name = "mp_payment_id", length = 120)
    private String mpPaymentId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommerceOrderItem> items = new ArrayList<>();

    public void addItem(Course course) {
        CommerceOrderItem item = new CommerceOrderItem(this, course);
        items.add(item);
    }

    public UUID getBuyerUserId() {
        return buyer != null ? buyer.getId() : null;
    }
}
