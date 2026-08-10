package com.infoprodutos.api.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.certificate.domain.Certificate;
import com.infoprodutos.api.certificate.domain.CertificateStatus;
import com.infoprodutos.api.certificate.dto.CertificateResponse;
import com.infoprodutos.api.certificate.pdf.CertificatePdfGenerator;
import com.infoprodutos.api.certificate.repository.CertificateRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.config.CertificateProperties;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.enrollment.EnrollmentService;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CertificateEligibilityService eligibilityService;

    @Mock
    private CertificatePdfGenerator pdfGenerator;

    @Mock
    private AuditService auditService;

    @Mock
    private com.infoprodutos.api.notification.NotificationService notificationService;

    @TempDir
    Path tempDir;

    private CertificateService service;
    private User student;
    private User other;
    private Course course;
    private Enrollment enrollment;
    private CustomUserDetails principal;

    @BeforeEach
    void setUp() throws Exception {
        CertificateProperties props = new CertificateProperties(
                tempDir.toString(), "http://localhost:3000", "Rafael Kienen", "Pedro Honorio");
        service = new CertificateService(
                certificateRepository,
                enrollmentService,
                enrollmentRepository,
                eligibilityService,
                pdfGenerator,
                props,
                auditService,
                notificationService);

        student = userWithRole(RoleCode.STUDENT, "aluno@test.local");
        other = userWithRole(RoleCode.STUDENT, "outro@test.local");
        principal = new CustomUserDetails(student);
        course = new Course("Valorant Master", "valorant", student);
        setId(course, UUID.randomUUID());
        course.setWorkloadHours(new BigDecimal("10.0"));
        course.setCertificateEnabled(true);
        enrollment = new Enrollment(student, course, student.getId());
        setId(enrollment, UUID.randomUUID());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setCompletedAt(Instant.parse("2026-08-01T12:00:00Z"));
    }

    @Test
    void completeCourse_requiresAllLessonsDone() {
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        when(eligibilityService.allPublishedLessonsCompleted(enrollment)).thenReturn(false);

        assertThatThrownBy(() -> service.completeCourse(enrollment.getId(), principal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("todas as aulas");
    }

    @Test
    void issue_returnsExistingWithoutDuplicating() throws Exception {
        Certificate existing = baseCertificate();
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        when(certificateRepository.findByEnrollmentId(enrollment.getId())).thenReturn(Optional.of(existing));

        CertificateResponse response = service.issue(enrollment.getId(), principal);

        assertThat(response.id()).isEqualTo(existing.getId().toString());
        verify(eligibilityService, never()).requireEligibleForCertificate(any());
        verify(certificateRepository, never()).save(any());
    }

    @Test
    void issue_createsPdfAndUniqueCode() throws Exception {
        when(enrollmentService.findOrThrow(enrollment.getId())).thenReturn(enrollment);
        when(certificateRepository.findByEnrollmentId(enrollment.getId())).thenReturn(Optional.empty());
        when(certificateRepository.existsByValidationCode(any())).thenReturn(false);
        when(certificateRepository.save(any())).thenAnswer(inv -> {
            Certificate c = inv.getArgument(0);
            if (c.getId() == null) {
                setId(c, UUID.randomUUID());
            }
            return c;
        });
        org.mockito.Mockito.doAnswer(inv -> {
                    OutputStream out = inv.getArgument(1);
                    out.write("%PDF-1.4 mock".getBytes());
                    return null;
                })
                .when(pdfGenerator)
                .generate(any(), any());

        CertificateResponse response = service.issue(enrollment.getId(), principal);

        assertThat(response.validationCode()).startsWith("CERT-");
        assertThat(response.validationUrl()).contains("/certificados/");
        assertThat(response.studentName()).isEqualTo(student.getName());
        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Certificate saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(saved.getPdfPath()).isNotBlank();
        assertThat(Files.exists(Path.of(saved.getPdfPath()))).isTrue();
    }

    @Test
    void get_rejectsOtherStudent() throws Exception {
        Certificate certificate = baseCertificate();
        when(certificateRepository.findById(certificate.getId())).thenReturn(Optional.of(certificate));

        assertThatThrownBy(() -> service.get(certificate.getId(), new CustomUserDetails(other)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void snapshot_keepsOriginalNames() throws Exception {
        Certificate certificate = baseCertificate();
        certificate.setStudentNameSnapshot("Nome Antigo");
        certificate.setCourseTitleSnapshot("Curso Antigo");
        when(certificateRepository.findById(certificate.getId())).thenReturn(Optional.of(certificate));

        // mutação pós-emissão no domínio vivo não afeta o snapshot
        student.setName("Nome Novo");
        course.setTitle("Curso Novo");

        CertificateResponse response = service.get(certificate.getId(), principal);
        assertThat(response.studentName()).isEqualTo("Nome Antigo");
        assertThat(response.courseTitle()).isEqualTo("Curso Antigo");
    }

    private Certificate baseCertificate() throws Exception {
        Certificate c = new Certificate();
        setId(c, UUID.randomUUID());
        c.setEnrollmentId(enrollment.getId());
        c.setStudentUserId(student.getId());
        c.setCourseId(course.getId());
        c.setValidationCode("CERT-2026-ABC123");
        c.setStatus(CertificateStatus.ISSUED);
        c.setStudentNameSnapshot(student.getName());
        c.setCourseTitleSnapshot(course.getTitle());
        c.setWorkloadHoursSnapshot(course.getWorkloadHours());
        c.setCompletionDate(java.time.LocalDate.of(2026, 8, 1));
        c.setIssuedAt(Instant.now());
        c.setCoordinatorNameSnapshot("Rafael Kienen");
        c.setChiefVisionOfficerNameSnapshot("Pedro Honorio");
        c.setValidationUrl("http://localhost:3000/certificados/CERT-2026-ABC123");
        return c;
    }

    private static User userWithRole(String roleCode, String email) throws Exception {
        Role role = new Role(roleCode, roleCode);
        setId(role, UUID.randomUUID());
        User user = new User("Aluno Teste", email, "hash");
        setId(user, UUID.randomUUID());
        user.setRoles(Set.of(role));
        return user;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Class<?> c = entity.getClass();
        Field idField = null;
        while (c != null) {
            try {
                idField = c.getDeclaredField("id");
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        if (idField == null) {
            throw new IllegalStateException("id field not found");
        }
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
