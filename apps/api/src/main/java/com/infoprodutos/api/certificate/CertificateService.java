package com.infoprodutos.api.certificate;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.certificate.domain.Certificate;
import com.infoprodutos.api.certificate.domain.CertificateStatus;
import com.infoprodutos.api.certificate.dto.CertificateResponse;
import com.infoprodutos.api.certificate.dto.PublicCertificateValidationResponse;
import com.infoprodutos.api.certificate.pdf.CertificatePdfGenerator;
import com.infoprodutos.api.certificate.pdf.CertificatePdfModel;
import com.infoprodutos.api.certificate.repository.CertificateRepository;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ConflictException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.config.CertificateProperties;
import com.infoprodutos.api.enrollment.EnrollmentService;
import com.infoprodutos.api.enrollment.domain.Enrollment;
import com.infoprodutos.api.enrollment.domain.EnrollmentStatus;
import com.infoprodutos.api.enrollment.repository.EnrollmentRepository;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.RoleCode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CertificateRepository certificateRepository;
    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificateEligibilityService eligibilityService;
    private final CertificatePdfGenerator pdfGenerator;
    private final CertificateProperties properties;
    private final AuditService auditService;

    @Transactional
    public Enrollment completeCourse(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = enrollmentService.findOrThrow(enrollmentId);
        requireOwnedStudent(enrollment, principal);
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Matrícula não está ativa.");
        }
        if (!eligibilityService.allPublishedLessonsCompleted(enrollment)) {
            throw new BadRequestException("Conclua todas as aulas publicadas antes de finalizar o curso.");
        }
        if (enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(Instant.now());
            enrollment = enrollmentRepository.save(enrollment);
            auditService.record(principal.getId(), "COURSE_COMPLETED", "Enrollment", enrollmentId, null);
        }
        return enrollment;
    }

    @Transactional
    public CertificateResponse issue(UUID enrollmentId, CustomUserDetails principal) {
        Enrollment enrollment = enrollmentService.findOrThrow(enrollmentId);
        requireOwnedStudent(enrollment, principal);

        var existing = certificateRepository.findByEnrollmentId(enrollmentId);
        if (existing.isPresent()) {
            return CertificateResponse.from(existing.get());
        }

        eligibilityService.requireEligibleForCertificate(enrollment);

        String code = nextUniqueCode();
        String validationUrl = properties.publicWebBaseUrl().replaceAll("/$", "") + "/certificados/" + code;

        Certificate certificate = new Certificate();
        certificate.setEnrollmentId(enrollment.getId());
        certificate.setStudentUserId(enrollment.getStudent().getId());
        certificate.setCourseId(enrollment.getCourse().getId());
        certificate.setValidationCode(code);
        certificate.setStatus(CertificateStatus.ISSUED);
        certificate.setStudentNameSnapshot(enrollment.getStudent().getName());
        certificate.setCourseTitleSnapshot(enrollment.getCourse().getTitle());
        certificate.setWorkloadHoursSnapshot(enrollment.getCourse().getWorkloadHours());
        certificate.setCompletionDate(
                enrollment.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate());
        certificate.setIssuedAt(Instant.now());
        certificate.setCoordinatorNameSnapshot(properties.coordinatorName());
        certificate.setChiefVisionOfficerNameSnapshot(properties.chiefVisionOfficerName());
        certificate.setValidationUrl(validationUrl);
        certificate = certificateRepository.save(certificate);

        Path pdfPath = writePdf(certificate);
        certificate.setPdfPath(pdfPath.toString());
        certificate = certificateRepository.save(certificate);

        auditService.record(principal.getId(), "CERTIFICATE_ISSUED", "Certificate", certificate.getId(), null);
        return CertificateResponse.from(certificate);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> listMine(CustomUserDetails principal) {
        return certificateRepository.findByStudentUserIdOrderByIssuedAtDesc(principal.getId()).stream()
                .map(CertificateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse get(UUID id, CustomUserDetails principal) {
        Certificate certificate =
                certificateRepository.findById(id).orElseThrow(() -> new NotFoundException("Certificado não encontrado."));
        requireViewAccess(certificate, principal);
        return CertificateResponse.from(certificate);
    }

    @Transactional
    public Resource loadPdf(UUID id, CustomUserDetails principal) {
        Certificate certificate =
                certificateRepository.findById(id).orElseThrow(() -> new NotFoundException("Certificado não encontrado."));
        requireViewAccess(certificate, principal);
        // Regenera o PDF no download para refletir o template atual (assinaturas/layout).
        Path path = writePdf(certificate);
        certificate.setPdfPath(path.toString());
        certificateRepository.save(certificate);
        return new FileSystemResource(path);
    }

    @Transactional(readOnly = true)
    public PublicCertificateValidationResponse validatePublic(String code) {
        Certificate certificate = certificateRepository
                .findByValidationCode(code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Certificado não encontrado."));
        boolean valid = certificate.getStatus() == CertificateStatus.ISSUED && certificate.getRevokedAt() == null;
        return new PublicCertificateValidationResponse(
                valid,
                certificate.getStatus().name(),
                certificate.getStudentNameSnapshot(),
                certificate.getCourseTitleSnapshot(),
                certificate.getWorkloadHoursSnapshot(),
                certificate.getCompletionDate(),
                certificate.getValidationCode(),
                certificate.getIssuedAt().atZone(ZoneOffset.UTC).toLocalDate());
    }

    private Path writePdf(Certificate certificate) {
        try {
            Path root = Path.of(properties.storageLocalRoot()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path file = root.resolve(certificate.getId() + ".pdf");
            CertificatePdfModel model = new CertificatePdfModel(
                    certificate.getStudentNameSnapshot(),
                    certificate.getCourseTitleSnapshot(),
                    certificate.getWorkloadHoursSnapshot(),
                    certificate.getCompletionDate(),
                    certificate.getValidationCode(),
                    certificate.getValidationUrl(),
                    certificate.getCoordinatorNameSnapshot(),
                    certificate.getChiefVisionOfficerNameSnapshot());
            try (OutputStream out = Files.newOutputStream(file)) {
                pdfGenerator.generate(model, out);
            }
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar o PDF do certificado.", e);
        }
    }

    private String nextUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = "CERT-" + Year.now(ZoneOffset.UTC).getValue() + "-" + randomSuffix(6);
            if (!certificateRepository.existsByValidationCode(code)) {
                return code;
            }
        }
        throw new ConflictException("Não foi possível gerar código de validação único.");
    }

    private static String randomSuffix(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private static void requireOwnedStudent(Enrollment enrollment, CustomUserDetails principal) {
        if (!enrollment.getStudent().getId().equals(principal.getId())) {
            throw new ForbiddenOperationException("Somente o aluno da matrícula pode executar esta ação.");
        }
    }

    private void requireViewAccess(Certificate certificate, CustomUserDetails principal) {
        if (certificate.getStudentUserId().equals(principal.getId())) {
            return;
        }
        if (principal.getRoleCodes().contains(RoleCode.SUPER_ADMIN)) {
            return;
        }
        throw new ForbiddenOperationException("Você não pode acessar o certificado de outro aluno.");
    }
}
