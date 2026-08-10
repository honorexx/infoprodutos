package com.infoprodutos.api.video.storage;

import com.infoprodutos.api.config.S3StorageProperties;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Storage S3-compatible (Cloudflare R2, AWS S3, MinIO).
 * Uploads grandes: browser → PUT assinado direto no bucket (sem passar pela API/Vercel).
 */
@Component
@ConditionalOnProperty(prefix = "app.video-storage.s3", name = "enabled", havingValue = "true")
public class S3CompatibleVideoStorageProvider implements VideoStorageProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(S3CompatibleVideoStorageProvider.class);

    private final S3StorageProperties properties;
    private final S3Client s3;
    private final S3Presigner presigner;

    public S3CompatibleVideoStorageProvider(S3StorageProperties properties) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "app.video-storage.s3.enabled=true mas faltam endpoint/bucket/access-key/secret-key.");
        }
        this.properties = properties;
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        Region region = Region.of(properties.region());
        URI endpoint = URI.create(properties.endpoint());

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                // R2 não gosta de Transfer-Encoding: chunked do SDK.
                .chunkedEncodingEnabled(false)
                .build();

        // AWS SDK 2.30+ assina checksum CRC32 por default — R2 rejeita PUT do browser
        // (header x-amz-checksum-* não enviado). WHEN_REQUIRED = compatível com R2.
        this.s3 = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .serviceConfiguration(s3Config)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .serviceConfiguration(s3Config)
                .build();

        log.info(
                "Video storage S3-compatible ativo: bucket={} endpoint={} (checksum=WHEN_REQUIRED)",
                properties.bucket(),
                properties.endpoint());
    }

    @Override
    public String providerName() {
        return "S3_COMPATIBLE";
    }

    @Override
    public boolean supportsDirectUpload() {
        return true;
    }

    @Override
    public String allocateKey(String keyPrefix, String originalFilename) {
        String safe = VideoStorageProvider.sanitizeFilename(originalFilename);
        return keyPrefix + "/" + UUID.randomUUID() + "_" + safe;
    }

    @Override
    public StoredObject store(
            String keyPrefix, String originalFilename, String contentType, InputStream data, long sizeBytes) {
        String key = allocateKey(keyPrefix, originalFilename);
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream");
        if (sizeBytes > 0) {
            req.contentLength(sizeBytes);
        }
        try {
            var response = s3.putObject(
                    req.build(),
                    sizeBytes > 0 ? RequestBody.fromInputStream(data, sizeBytes) : RequestBody.fromBytes(data.readAllBytes()));
            String etag = response.eTag() != null ? response.eTag().replace("\"", "") : null;
            long stored = sizeBytes > 0 ? sizeBytes : head(key).sizeBytes();
            return new StoredObject(key, stored, etag);
        } catch (Exception e) {
            try {
                delete(key);
            } catch (Exception ignored) {
                // best-effort
            }
            throw new IllegalStateException("Falha ao armazenar objeto no S3/R2", e);
        }
    }

    @Override
    public InputStream open(String storageKey) {
        try {
            return s3.getObject(GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new IllegalStateException("Objeto não encontrado no S3/R2: " + storageKey, e);
        } catch (S3Exception e) {
            throw new IllegalStateException("Falha ao abrir objeto no S3/R2: " + storageKey, e);
        }
    }

    @Override
    public Path resolvePath(String storageKey) {
        throw new UnsupportedOperationException("S3/R2 não expõe Path local");
    }

    @Override
    public void delete(String storageKey) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
        } catch (S3Exception e) {
            throw new IllegalStateException("Falha ao remover objeto do S3/R2", e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            head(storageKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ObjectStat head(String storageKey) {
        try {
            HeadObjectResponse response = s3.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
            Long len = response.contentLength();
            return new ObjectStat(
                    len != null ? len : 0L,
                    response.contentType(),
                    response.eTag() != null ? response.eTag().replace("\"", "") : null);
        } catch (NoSuchKeyException e) {
            throw new IllegalStateException("Objeto não encontrado no S3/R2: " + storageKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IllegalStateException("Objeto não encontrado no S3/R2: " + storageKey, e);
            }
            throw new IllegalStateException("Falha ao consultar objeto no S3/R2: " + storageKey, e);
        }
    }

    @Override
    public PresignedPut createPresignedPut(String storageKey, String contentType, Duration ttl) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .build();
        var presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(objectRequest)
                .build());
        // Content-Type deve ser enviado exatamente como assinado.
        return new PresignedPut(presigned.url().toString(), "PUT", Map.of("Content-Type", contentType));
    }

    @Override
    public String createPresignedGet(String storageKey, Duration ttl) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(objectRequest)
                .build());
        return presigned.url().toString();
    }

    public int uploadUrlTtlSeconds() {
        return properties.uploadUrlTtlSeconds();
    }

    public int downloadUrlTtlSeconds() {
        return properties.downloadUrlTtlSeconds();
    }

    @Override
    public void close() {
        s3.close();
        presigner.close();
    }
}
