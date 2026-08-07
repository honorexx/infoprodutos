package com.infoprodutos.api.course.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import com.infoprodutos.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_instructor")
@Getter
@Setter
@NoArgsConstructor
public class CourseInstructor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_user_id", nullable = false)
    private User instructor;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    public CourseInstructor(Course course, User instructor, boolean primary) {
        this.course = course;
        this.instructor = instructor;
        this.primary = primary;
    }
}
