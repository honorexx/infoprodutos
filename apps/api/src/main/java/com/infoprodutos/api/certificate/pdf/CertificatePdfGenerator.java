package com.infoprodutos.api.certificate.pdf;

import java.io.OutputStream;

public interface CertificatePdfGenerator {
    void generate(CertificatePdfModel model, OutputStream out);
}
