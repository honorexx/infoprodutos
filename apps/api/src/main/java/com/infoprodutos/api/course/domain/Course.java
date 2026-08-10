package com.infoprodutos.api.course.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import com.infoprodutos.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
public class Course extends AuditableEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "cover_mime_type", length = 100)
    private String coverMimeType;

    @Column(name = "workload_hours", precision = 6, scale = 2)
    private BigDecimal workloadHours;

    /** Preço em centavos (BRL). 0 = sem compra self-service (só grant manual). */
    @Column(name = "price_cents", nullable = false)
    private long priceCents = 0L;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "min_completion_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minCompletionPercentage = new BigDecimal("100");

    @Column(name = "min_passing_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPassingScore = new BigDecimal("70");

    @Column(name = "certificate_enabled", nullable = false)
    private boolean certificateEnabled = true;

    @Column(name = "max_quiz_attempts")
    private Integer maxQuizAttempts;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Course(String title, String slug, User createdBy) {
        this.title = title;
        this.slug = slug;
        this.createdBy = createdBy;
    }

    public boolean isActive() {
        return deletedAt == null;
    }
}
