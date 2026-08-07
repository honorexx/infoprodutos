package com.infoprodutos.api.certificate;

import com.infoprodutos.api.certificate.dto.CertificateResponse;
import com.infoprodutos.api.certificate.dto.PublicCertificateValidationResponse;
import com.infoprodutos.api.enrollment.dto.EnrollmentResponse;
import com.infoprodutos.api.security.CustomUserDetails;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/api/v1/enrollments/{id}/complete-course")
    @PreAuthorize("isAuthenticated()")
    public EnrollmentResponse completeCourse(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return EnrollmentResponse.from(certificateService.completeCourse(id, principal));
    }

    @PostMapping("/api/v1/enrollments/{id}/certificate/issue")
    @PreAuthorize("isAuthenticated()")
    public CertificateResponse issue(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return certificateService.issue(id, principal);
    }

    @GetMapping("/api/v1/certificates/me")
    @PreAuthorize("isAuthenticated()")
    public List<CertificateResponse> mine(@AuthenticationPrincipal CustomUserDetails principal) {
        return certificateService.listMine(principal);
    }

    @GetMapping("/api/v1/certificates/{id}")
    @PreAuthorize("isAuthenticated()")
    public CertificateResponse get(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return certificateService.get(id, principal);
    }

    @GetMapping("/api/v1/certificates/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> pdf(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        Resource resource = certificateService.loadPdf(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-" + id + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/api/v1/public/certificates/validate/{code}")
    public PublicCertificateValidationResponse validatePublic(@PathVariable String code) {
        return certificateService.validatePublic(code);
    }
}
