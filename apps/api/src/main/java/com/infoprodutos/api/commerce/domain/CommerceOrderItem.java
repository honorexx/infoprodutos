package com.infoprodutos.api.commerce.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import com.infoprodutos.api.course.domain.Course;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commerce_order_item")
@Getter
@Setter
@NoArgsConstructor
public class CommerceOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public CommerceOrderItem(CommerceOrder order, Course course) {
        this.order = order;
        this.course = course;
    }
}
