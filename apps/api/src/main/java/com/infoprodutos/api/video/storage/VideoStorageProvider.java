package com.infoprodutos.api.video.storage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Abstração de armazenamento de vídeo/materiais. Troca de provedor = nova
 * implementação + configuração, sem alterar o domínio.
 */
public interface VideoStorageProvider {

    String providerName();

    StoredObject store(String keyPrefix, String originalFilename, String contentType, InputStream data, long sizeBytes);

    InputStream open(String storageKey);

    Path resolvePath(String storageKey);

    void delete(String storageKey);

    record StoredObject(String storageKey, long sizeBytes, String checksum) {}
}
