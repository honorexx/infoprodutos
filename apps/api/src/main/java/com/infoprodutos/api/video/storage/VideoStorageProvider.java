package com.infoprodutos.api.video.storage;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Abstração de armazenamento de vídeo/materiais. Troca de provedor = nova
 * implementação + configuração, sem alterar o domínio.
 */
public interface VideoStorageProvider {

    String providerName();

    /** Quando true, o browser faz PUT direto no object storage via URL assinada. */
    default boolean supportsDirectUpload() {
        return false;
    }

    StoredObject store(String keyPrefix, String originalFilename, String contentType, InputStream data, long sizeBytes);

    /** Aloca uma storage key estável (sem upload) — usado no fluxo DIRECT. */
    default String allocateKey(String keyPrefix, String originalFilename) {
        String safe = sanitizeFilename(originalFilename);
        return keyPrefix + "/" + java.util.UUID.randomUUID() + "_" + safe;
    }

    InputStream open(String storageKey);

    Path resolvePath(String storageKey);

    void delete(String storageKey);

    default boolean exists(String storageKey) {
        try (InputStream ignored = open(storageKey)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    default ObjectStat head(String storageKey) {
        throw new UnsupportedOperationException("head não suportado por " + providerName());
    }

    default PresignedPut createPresignedPut(String storageKey, String contentType, Duration ttl) {
        throw new UnsupportedOperationException("Presigned PUT não suportado por " + providerName());
    }

    default String createPresignedGet(String storageKey, Duration ttl) {
        throw new UnsupportedOperationException("Presigned GET não suportado por " + providerName());
    }

    static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file.bin";
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.length() > 120 ? cleaned.substring(cleaned.length() - 120) : cleaned;
    }

    record StoredObject(String storageKey, long sizeBytes, String checksum) {}

    record ObjectStat(long sizeBytes, String contentType, String eTag) {}

    record PresignedPut(String uploadUrl, String httpMethod, Map<String, String> requiredHeaders) {}
}
