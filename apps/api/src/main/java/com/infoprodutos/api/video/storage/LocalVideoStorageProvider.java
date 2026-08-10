package com.infoprodutos.api.video.storage;

import com.infoprodutos.api.config.VideoStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalVideoStorageProvider implements VideoStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalVideoStorageProvider.class);

    private final Path root;

    public LocalVideoStorageProvider(VideoStorageProperties properties) {
        this.root = resolveRoot(properties.localRoot());
        try {
            Files.createDirectories(this.root);
            log.info("Video storage local root: {}", this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório local de vídeos: " + root, e);
        }
    }

    /**
     * Paths relativos resolvem para {@code <monorepo>/data/videos} tanto se o JVM
     * subir em {@code apps/api} quanto na raiz do repositório — evita capas “sumirem”
     * por cwd diferente (IntelliJ vs Maven).
     */
    static Path resolveRoot(String configured) {
        Path raw = Path.of(configured == null || configured.isBlank() ? "./data/videos" : configured);
        if (raw.isAbsolute()) {
            return raw.normalize();
        }
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path monorepoRoot = detectMonorepoRoot(cwd);
        String relative = raw.toString().replaceFirst("^\\./", "");
        return monorepoRoot.resolve(relative).normalize();
    }

    static Path detectMonorepoRoot(Path cwd) {
        if (cwd.getFileName() != null
                && cwd.getFileName().toString().equals("api")
                && cwd.getParent() != null
                && cwd.getParent().getFileName() != null
                && cwd.getParent().getFileName().toString().equals("apps")
                && cwd.getParent().getParent() != null) {
            return cwd.getParent().getParent();
        }
        if (Files.isDirectory(cwd.resolve("apps/api")) && Files.isDirectory(cwd.resolve("apps/web"))) {
            return cwd;
        }
        return cwd;
    }

    @Override
    public String providerName() {
        return "LOCAL_DEV";
    }

    @Override
    public StoredObject store(String keyPrefix, String originalFilename, String contentType, InputStream data, long sizeBytes) {
        String safeName = sanitizeFilename(originalFilename);
        String key = keyPrefix + "/" + UUID.randomUUID() + "_" + safeName;
        Path target = resolvePath(key);
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream din = new DigestInputStream(data, digest);
                    OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                din.transferTo(out);
            }
            long storedSize = Files.size(target);
            String checksum = HexFormat.of().formatHex(digest.digest());
            return new StoredObject(key, storedSize, checksum);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
            throw new IllegalStateException("Falha ao armazenar arquivo localmente", e);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        try {
            return Files.newInputStream(resolvePath(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("Arquivo não encontrado no storage local", e);
        }
    }

    @Override
    public Path resolvePath(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("storage_key inválida");
        }
        return resolved;
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolvePath(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao remover arquivo do storage", e);
        }
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "file.bin";
        }
        String cleaned = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.length() > 120 ? cleaned.substring(cleaned.length() - 120) : cleaned;
    }
}
