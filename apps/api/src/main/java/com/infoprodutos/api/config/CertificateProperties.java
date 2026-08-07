package com.infoprodutos.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.certificate")
public record CertificateProperties(
        String storageLocalRoot,
        String publicWebBaseUrl,
        String coordinatorName,
        String chiefVisionOfficerName) {

    public CertificateProperties {
        if (storageLocalRoot == null || storageLocalRoot.isBlank()) {
            storageLocalRoot = "./data/certificates";
        }
        if (publicWebBaseUrl == null || publicWebBaseUrl.isBlank()) {
            publicWebBaseUrl = "http://localhost:3000";
        }
        if (coordinatorName == null || coordinatorName.isBlank()) {
            coordinatorName = "Rafael Kienen";
        }
        if (chiefVisionOfficerName == null || chiefVisionOfficerName.isBlank()) {
            chiefVisionOfficerName = "Pedro Honorio";
        }
    }
}
