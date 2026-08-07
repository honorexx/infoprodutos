package com.infoprodutos.api.enrollment.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enrollment")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    public Enrollment(User student, Course course, UUID grantedByUserId) {
        this.student = student;
        this.course = course;
        this.grantedByUserId = grantedByUserId;
        this.status = EnrollmentStatus.ACTIVE;
        this.startedAt = Instant.now();
    }
}
