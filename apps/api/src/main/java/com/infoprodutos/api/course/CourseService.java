package com.infoprodutos.api.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.CourseInstructor;
import com.infoprodutos.api.course.domain.CourseStatus;
import com.infoprodutos.api.course.dto.CourseCreateRequest;
import com.infoprodutos.api.course.dto.CourseResponse;
import com.infoprodutos.api.course.dto.CourseSummaryResponse;
import com.infoprodutos.api.course.dto.CourseUpdateRequest;
import com.infoprodutos.api.course.repository.CourseInstructorRepository;
import com.infoprodutos.api.course.repository.CourseRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final CourseAccessGuard accessGuard;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> list(Pageable pageable, CustomUserDetails principal) {
        List<String> roles = principal.getRoleCodes();
        Page<Course> page;
        if (roles.contains(RoleCode.SUPER_ADMIN)) {
            page = courseRepository.findAllActive(pageable);
        } else if (roles.contains(RoleCode.INSTRUCTOR)) {
            page = courseRepository.findAllActiveByInstructor(principal.getId(), pageable);
        } else {
            page = courseRepository.findAllActiveByStatus(CourseStatus.PUBLISHED, pageable);
        }
        return page.map(CourseSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public CourseResponse get(UUID id, CustomUserDetails principal) {
        Course course = findActiveOrThrow(id);
        requireViewAccess(course, principal);
        return CourseResponse.from(course, courseInstructorRepository.findByCourseId(id));
    }

    @Transactional
    public CourseResponse create(CourseCreateRequest request, CustomUserDetails principal) {
        User creator = userRepository
                .findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        String slug = (request.slug() == null || request.slug().isBlank())
                ? Slugifier.slugify(request.title())
                : Slugifier.slugify(request.slug());
        if (slug.isBlank()) {
            throw new BadRequestException("Não foi possível gerar um slug a partir do título informado.");
        }
        slug = ensureUniqueSlug(slug);

        Course course = new Course(request.title().trim(), slug, creator);
        course.setDescription(request.description());
        course.setWorkloadHours(request.workloadHours());
        course = courseRepository.save(course);

        if (principal.getRoleCodes().contains(RoleCode.INSTRUCTOR)) {
            courseInstructorRepository.save(new CourseInstructor(course, creator, true));
        }

        auditService.record(
                principal.getId(), "COURSE_CREATED", "Course", course.getId(), toJson(Map.of("title", course.getTitle())));
        return CourseResponse.from(course, courseInstructorRepository.findByCourseId(course.getId()));
    }

    @Transactional
    public CourseResponse update(UUID id, CourseUpdateRequest request, CustomUserDetails principal) {
        Course course = findActiveOrThrow(id);
        accessGuard.requireManageAccess(id, principal);

        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setCoverImageUrl(request.coverImageUrl());
        course.setWorkloadHours(request.workloadHours());
        if (request.minCompletionPercentage() != null) {
            course.setMinCompletionPercentage(request.minCompletionPercentage());
        }
        if (request.minPassingScore() != null) {
            course.setMinPassingScore(request.minPassingScore());
        }
        course.setCertificateEnabled(request.certificateEnabled());
        course.setMaxQuizAttempts(request.maxQuizAttempts());
        course = courseRepository.save(course);

        auditService.record(principal.getId(), "COURSE_UPDATED", "Course", course.getId(), null);
        return CourseResponse.from(course, courseInstructorRepository.findByCourseId(course.getId()));
    }

    @Transactional
    public void publish(UUID id, CustomUserDetails principal) {
        Course course = findActiveOrThrow(id);
        accessGuard.requireManageAccess(id, principal);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ConflictException("Curso arquivado não pode ser publicado diretamente.");
        }
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return;
        }
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(Instant.now());
        courseRepository.save(course);
        auditService.record(principal.getId(), "COURSE_PUBLISHED", "Course", course.getId(), null);
    }

    @Transactional
    public void unpublish(UUID id, CustomUserDetails principal) {
        Course course = findActiveOrThrow(id);
        accessGuard.requireManageAccess(id, principal);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ConflictException("Curso arquivado não pode ser despublicado.");
        }
        if (course.getStatus() == CourseStatus.DRAFT) {
            return;
        }
        course.setStatus(CourseStatus.DRAFT);
        courseRepository.save(course);
        auditService.record(principal.getId(), "COURSE_UNPUBLISHED", "Course", course.getId(), null);
    }

    @Transactional
    public void archive(UUID id, CustomUserDetails principal) {
        Course course = findActiveOrThrow(id);
        accessGuard.requireManageAccess(id, principal);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            return;
        }
        course.setStatus(CourseStatus.ARCHIVED);
        course.setArchivedAt(Instant.now());
        courseRepository.save(course);
        auditService.record(principal.getId(), "COURSE_ARCHIVED", "Course", course.getId(), null);
    }

    @Transactional
    public void delete(UUID id, CustomUserDetails principal) {
        Course course = findActiveOrThrow(id);
        course.setDeletedAt(Instant.now());
        courseRepository.save(course);
        auditService.record(principal.getId(), "COURSE_DELETED", "Course", course.getId(), null);
    }

    @Transactional
    public void addInstructor(UUID courseId, UUID instructorUserId, boolean primary, CustomUserDetails principal) {
        Course course = findActiveOrThrow(courseId);
        accessGuard.requireManageAccess(courseId, principal);
        User instructor = userRepository
                .findById(instructorUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        if (!instructor.hasRole(RoleCode.INSTRUCTOR)) {
            throw new BadRequestException("Usuário informado não possui o papel INSTRUCTOR.");
        }
        if (courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, instructorUserId)) {
            return;
        }
        courseInstructorRepository.save(new CourseInstructor(course, instructor, primary));
        auditService.record(
                principal.getId(),
                "COURSE_INSTRUCTOR_ADDED",
                "Course",
                courseId,
                toJson(Map.of("instructorUserId", instructorUserId.toString())));
    }

    void requireViewAccess(Course course, CustomUserDetails principal) {
        List<String> roles = principal.getRoleCodes();
        if (roles.contains(RoleCode.SUPER_ADMIN)) {
            return;
        }
        if (roles.contains(RoleCode.INSTRUCTOR)
                && courseInstructorRepository.existsByCourseIdAndInstructorId(course.getId(), principal.getId())) {
            return;
        }
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            return;
        }
        throw new ForbiddenOperationException("Você não tem permissão para visualizar este curso.");
    }

    Course findActiveOrThrow(UUID id) {
        return courseRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Curso não encontrado."));
    }

    private String ensureUniqueSlug(String baseSlug) {
        String candidate = baseSlug;
        int suffix = 2;
        while (courseRepository.existsBySlugAndDeletedAtIsNull(candidate)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
