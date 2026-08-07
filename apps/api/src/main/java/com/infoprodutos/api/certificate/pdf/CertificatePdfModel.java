package com.infoprodutos.api.certificate.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados imutáveis usados apenas para renderizar o PDF. */
public record CertificatePdfModel(
        String studentName,
        String courseTitle,
        BigDecimal workloadHours,
        LocalDate completionDate,
        String validationCode,
        String validationUrl,
        String coordinatorName,
        String chiefVisionOfficerName) {}
