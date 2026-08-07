package com.infoprodutos.api.certificate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta pública — apenas dados permitidos (SECURITY.md). */
public record PublicCertificateValidationResponse(
        boolean valid,
        String status,
        String studentName,
        String courseTitle,
        BigDecimal workloadHours,
        LocalDate completionDate,
        String validationCode,
        LocalDate issuedDate) {}
