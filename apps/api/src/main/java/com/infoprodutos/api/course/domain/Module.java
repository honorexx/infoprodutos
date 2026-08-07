package com.infoprodutos.api.course.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nome da classe é "Module" (não "CourseModule") pois vive em pacote próprio
 * de domínio (com.infoprodutos.api.course.domain); tabela chama-se "module".
 */
@Entity
@Table(name = "module")
@Getter
@Setter
@NoArgsConstructor
public class Module extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ModuleStatus status = ModuleStatus.DRAFT;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Module(Course course, String title, int orderIndex) {
        this.course = course;
        this.title = title;
        this.orderIndex = orderIndex;
    }
}
