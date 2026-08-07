package com.infoprodutos.api.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.CourseService;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.dto.CreateEnrollmentRequest;
import com.infoprodutos.api.enrollment.dto.EnrollmentResponse;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.user.repository.UserRepository;
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
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final CourseAccessGuard courseAccessGuard;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public EnrollmentResponse grant(CreateEnrollmentRequest request, CustomUserDetails principal) {
        Course course = courseService.findActiveOrThrow(request.courseId());
        courseAccessGuard.requireManageAccess(course.getId(), principal);

        User student = resolveStudent(request);
        if (!student.hasRole(RoleCode.STUDENT) && !student.hasRole(RoleCode.INSTRUCTOR) && !student.hasRole(RoleCode.SUPER_ADMIN)) {
            throw new BadRequestException("Usuário informado não pode ser matriculado.");
        }

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElse(null);

        if (enrollment == null) {
            enrollment = new Enrollment(student, course, principal.getId());
            enrollment = enrollmentRepository.save(enrollment);
            auditService.record(
                    principal.getId(),
                    "ENROLLMENT_GRANTED",
                    "Enrollment",
                    enrollment.getId(),
                    toJson(Map.of(
                            "courseId", course.getId().toString(),
                            "studentUserId", student.getId().toString())));
        } else if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            // Idempotente: já ativo.
        } else {
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setStartedAt(java.time.Instant.now());
            enrollment.setGrantedByUserId(principal.getId());
            enrollment.setExpiresAt(null);
            enrollment = enrollmentRepository.save(enrollment);
            auditService.record(
                    principal.getId(),
                    "ENROLLMENT_REACTIVATED",
                    "Enrollment",
                    enrollment.getId(),
                    null);
        }

        return EnrollmentResponse.from(enrollmentRepository.findByIdWithDetails(enrollment.getId()).orElse(enrollment));
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> list(
            UUID courseId, UUID studentId, EnrollmentStatus status, Pageable pageable, CustomUserDetails principal) {
        if (courseId != null) {
            courseAccessGuard.requireManageAccess(courseId, principal);
        } else if (!principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)) {
            throw new ForbiddenOperationException("Informe courseId para listar matrículas dos seus cursos.");
        }

        return enrollmentRepository.findFiltered(courseId, studentId, status, pageable).map(EnrollmentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listMine(CustomUserDetails principal) {
        return enrollmentRepository.findAllByStudentIdWithDetails(principal.getId()).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    @Transactional
    public EnrollmentResponse suspend(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = findOrThrow(enrollmentId);
        courseAccessGuard.requireManageAccess(enrollment.getCourse().getId(), principal);
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Somente matrículas ativas podem ser suspensas.");
        }
        enrollment.setStatus(EnrollmentStatus.SUSPENDED);
        enrollment = enrollmentRepository.save(enrollment);
        auditService.record(principal.getId(), "ENROLLMENT_SUSPENDED", "Enrollment", enrollment.getId(), null);
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse cancel(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = findOrThrow(enrollmentId);
        courseAccessGuard.requireManageAccess(enrollment.getCourse().getId(), principal);
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            return EnrollmentResponse.from(enrollment);
        }
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        enrollment = enrollmentRepository.save(enrollment);
        auditService.record(principal.getId(), "ENROLLMENT_CANCELLED", "Enrollment", enrollment.getId(), null);
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse reactivate(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = findOrThrow(enrollmentId);
        courseAccessGuard.requireManageAccess(enrollment.getCourse().getId(), principal);
        if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
            return EnrollmentResponse.from(enrollment);
        }
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setStartedAt(java.time.Instant.now());
        enrollment.setGrantedByUserId(principal.getId());
        enrollment = enrollmentRepository.save(enrollment);
        auditService.record(principal.getId(), "ENROLLMENT_REACTIVATED", "Enrollment", enrollment.getId(), null);
        return EnrollmentResponse.from(enrollment);
    }

    public Enrollment findOrThrow(UUID id) {
        return enrollmentRepository
                .findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Matrícula não encontrada."));
    }

    private User resolveStudent(CreateEnrollmentRequest request) {
        if (request.studentUserId() != null) {
            return userRepository
                    .findById(request.studentUserId())
                    .filter(u -> u.getDeletedAt() == null)
                    .orElseThrow(() -> new NotFoundException("Aluno não encontrado."));
        }
        if (request.studentEmail() != null && !request.studentEmail().isBlank()) {
            return userRepository
                    .findActiveByEmailIgnoreCase(request.studentEmail().trim())
                    .orElseThrow(() -> new NotFoundException("Aluno não encontrado para o e-mail informado."));
        }
        throw new BadRequestException("Informe studentUserId ou studentEmail.");
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
