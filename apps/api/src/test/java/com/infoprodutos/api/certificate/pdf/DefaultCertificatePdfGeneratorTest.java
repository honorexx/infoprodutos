package com.infoprodutos.api.certificate.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DefaultCertificatePdfGeneratorTest {

    private final DefaultCertificatePdfGenerator generator = new DefaultCertificatePdfGenerator();

    @Test
    void generatesNonEmptyPdf() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(sample("Ana Silva", "Curso de Java"), out);
        assertThat(out.size()).isGreaterThan(500);
        assertThat(out.toByteArray()).startsWith("%PDF".getBytes());
    }

    @Test
    void handlesVeryLongNamesWithoutThrowing() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String longName = "Maria ".repeat(20) + "da Silva Souza Oliveira";
        String longCourse = "Formação Completa em ".repeat(8) + "Engenharia de Software";
        generator.generate(sample(longName, longCourse), out);
        assertThat(out.size()).isGreaterThan(500);
    }

    private static CertificatePdfModel sample(String student, String course) {
        return new CertificatePdfModel(
                student,
                course,
                new BigDecimal("12.5"),
                LocalDate.of(2026, 8, 7),
                "CERT-2026-TEST01",
                "http://localhost:3000/certificados/CERT-2026-TEST01",
                "Rafael Kienen",
                "Pedro Honorio");
    }
}
