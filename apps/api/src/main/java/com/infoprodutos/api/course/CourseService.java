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
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
import com.infoprodutos.api.video.storage.VideoStorageProvider;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final Set<String> ALLOWED_COVER_MIME =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;

    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseAccessGuard accessGuard;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final VideoStorageProvider storageProvider;

    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> list(Pageable pageable, CustomUserDetails principal, String query) {
        String q = normalizeQuery(query);
        List<String> roles = principal.getRoleCodes();
        Page<Course> page;
        if (roles.contains(RoleCode.SUPER_ADMIN)) {
            page = q == null ? courseRepository.findAllActive(pageable) : courseRepository.searchAllActive(q, pageable);
        } else if (roles.contains(RoleCode.INSTRUCTOR)) {
            page = q == null
                    ? courseRepository.findAllActiveByInstructor(principal.getId(), pageable)
                    : courseRepository.searchAllActiveByInstructor(principal.getId(), q, pageable);
        } else if (q != null) {
            // Aluno com busca: só entre cursos matriculados (ex.: achar um entre centenas em "Meus cursos").
            page = courseRepository.searchActiveByStudentEnrollment(principal.getId(), q, pageable);
        } else {
            page = courseRepository.findAllActiveByStatus(CourseStatus.PUBLISHED, pageable);
        }
        return page.map(CourseSummaryResponse::from);
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        // Remove curingas do LIKE para a busca ser literal no prefixo das palavras.
        return trimmed.replace("%", "").replace("_", "");
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
        course.setPriceCents(request.priceCents() != null ? request.priceCents() : 0L);
        course.setCurrency("BRL");
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
        applyCoverImageUrlUpdate(course, request.coverImageUrl());
        course.setWorkloadHours(request.workloadHours());
        if (request.priceCents() != null) {
            course.setPriceCents(request.priceCents());
        }
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
        if (course.getPriceCents() <= 0) {
            throw new BadRequestException(
                    "Defina um preço maior que zero antes de publicar. O curso precisa ter valor para aparecer na vitrine.");
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

    @Transactional
    public CourseResponse uploadCover(UUID courseId, MultipartFile file, CustomUserDetails principal) {
        Course course = findActiveOrThrow(courseId);
        accessGuard.requireManageAccess(courseId, principal);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo de capa obrigatório.");
        }
        if (file.getSize() > MAX_COVER_BYTES) {
            throw new BadRequestException("Capa excede 5 MB. Envie uma imagem menor.");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean ok = ALLOWED_COVER_MIME.contains(contentType)
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp");
        if (!ok) {
            throw new BadRequestException("Capa inválida. Use JPG, PNG ou WebP.");
        }
        if (!ALLOWED_COVER_MIME.contains(contentType)) {
            if (name.endsWith(".png")) {
                contentType = "image/png";
            } else if (name.endsWith(".webp")) {
                contentType = "image/webp";
            } else {
                contentType = "image/jpeg";
            }
        }

        try (InputStream in = file.getInputStream()) {
            var stored = storageProvider.store(
                    "courses/" + courseId + "/covers",
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "cover.jpg",
                    contentType,
                    in,
                    file.getSize());
            course.setCoverImageUrl(stored.storageKey());
            course.setCoverMimeType(contentType);
            courseRepository.saveAndFlush(course);
            auditService.record(principal.getId(), "COURSE_COVER_UPLOADED", "Course", courseId, null);
            Course reloaded = findActiveOrThrow(courseId);
            return CourseResponse.from(reloaded, courseInstructorRepository.findByCourseId(courseId));
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(
                    "Não foi possível salvar a capa do curso: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> coverImage(UUID courseId, CustomUserDetails principal) {
        Course course = findActiveOrThrow(courseId);
        requireCoverViewAccess(course, principal);

        String raw = course.getCoverImageUrl();
        if (raw == null || raw.isBlank()) {
            throw new NotFoundException("Curso sem capa.");
        }
        if (CourseCoverUrls.isExternalUrl(raw)) {
            throw new BadRequestException("Capa externa — use a URL retornada em coverImageUrl.");
        }

        final InputStream in;
        try {
            in = storageProvider.open(raw);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new NotFoundException("Arquivo de capa não encontrado.");
        }
        MediaType mediaType = course.getCoverMimeType() != null
                ? MediaType.parseMediaType(course.getCoverMimeType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .contentType(mediaType)
                .body(new InputStreamResource(in));
    }

    void requireCoverViewAccess(Course course, CustomUserDetails principal) {
        if (accessGuard.canManage(course.getId(), principal)) {
            return;
        }
        if (enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                principal.getId(), course.getId(), EnrollmentStatus.ACTIVE)) {
            return;
        }
        // Alunos com matrícula não ativa ainda veem a capa na lista "Meus cursos".
        if (enrollmentRepository.findByStudentIdAndCourseId(principal.getId(), course.getId()).isPresent()) {
            return;
        }
        requireViewAccess(course, principal);
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

    public Course findActiveOrThrow(UUID id) {
        return courseRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Curso não encontrado."));
    }

    /**
     * Atualiza capa via PUT só para URL http(s) ou limpeza.
     * Paths resolvidos (`/courses/{id}/cover`) não sobrescrevem a storage_key local.
     */
    private void applyCoverImageUrlUpdate(Course course, String incoming) {
        if (incoming == null) {
            return;
        }
        if (incoming.isBlank()) {
            course.setCoverImageUrl(null);
            course.setCoverMimeType(null);
            return;
        }
        if (CourseCoverUrls.isExternalUrl(incoming)) {
            course.setCoverImageUrl(incoming.trim());
            course.setCoverMimeType(null);
            return;
        }
        // Path da API ou storage_key já persistida — não alterar no PUT textual.
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
