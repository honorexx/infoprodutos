package com.infoprodutos.api.video;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.ForbiddenOperationException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.config.ApiUrlProperties;
import com.infoprodutos.api.config.S3StorageProperties;
import com.infoprodutos.api.config.VideoStorageProperties;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.enrollment.EnrollmentAccessGuard;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.video.domain.ProcessingStatus;
import com.infoprodutos.api.video.domain.StorageProviderType;
import com.infoprodutos.api.video.domain.UploadStatus;
import com.infoprodutos.api.video.domain.VideoAsset;
import com.infoprodutos.api.video.dto.StreamUrlResponse;
import com.infoprodutos.api.video.dto.UploadInitRequest;
import com.infoprodutos.api.video.dto.UploadInitResponse;
import com.infoprodutos.api.video.dto.VideoAssetResponse;
import com.infoprodutos.api.video.repository.VideoAssetRepository;
import com.infoprodutos.api.video.storage.S3CompatibleVideoStorageProvider;
import com.infoprodutos.api.video.storage.VideoStorageProvider;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "application/octet-stream");

    private static final Set<String> ALLOWED_THUMBNAIL_MIME =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;

    private final VideoAssetRepository videoAssetRepository;
    private final LessonRepository lessonRepository;
    private final LessonService lessonService;
    private final CourseAccessGuard accessGuard;
    private final EnrollmentAccessGuard enrollmentAccessGuard;
    private final VideoStorageProvider storageProvider;
    private final VideoStorageProperties storageProperties;
    private final S3StorageProperties s3StorageProperties;
    private final StreamUrlSigner streamUrlSigner;
    private final ApiUrlProperties apiUrlProperties;
    private final AuditService auditService;

    @Transactional
    public UploadInitResponse initUpload(UploadInitRequest request, CustomUserDetails principal) {
        Lesson lesson = lessonService.findActiveOrThrow(request.lessonId());
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);

        boolean direct = storageProvider.supportsDirectUpload();

        VideoAsset asset = new VideoAsset();
        asset.setLessonId(request.lessonId());
        asset.setStorageProvider(
                direct ? StorageProviderType.S3_COMPATIBLE : StorageProviderType.LOCAL_DEV);
        asset.setUploadStatus(UploadStatus.PENDING);
        asset.setProcessingStatus(ProcessingStatus.PENDING);

        if (direct) {
            String videoCt = normalizeVideoContentType(request.videoContentType(), request.videoFilename());
            String thumbCt = normalizeThumbnailContentType(
                    request.thumbnailContentType(), request.thumbnailFilename());
            long videoSize = request.videoSizeBytes() != null ? request.videoSizeBytes() : 0L;
            long thumbSize = request.thumbnailSizeBytes() != null ? request.thumbnailSizeBytes() : 0L;
            if (videoSize <= 0) {
                throw new BadRequestException("Informe o tamanho do vídeo (videoSizeBytes).");
            }
            if (videoSize > storageProperties.maxFileBytes()) {
                throw new BadRequestException("Arquivo excede o tamanho máximo permitido.");
            }
            if (thumbSize <= 0) {
                throw new BadRequestException("Informe o tamanho da thumbnail (thumbnailSizeBytes).");
            }
            if (thumbSize > MAX_THUMBNAIL_BYTES) {
                throw new BadRequestException("Thumbnail excede 5 MB. Envie uma imagem menor.");
            }
            String videoName =
                    request.videoFilename() != null && !request.videoFilename().isBlank()
                            ? request.videoFilename()
                            : "video.mp4";
            String thumbName =
                    request.thumbnailFilename() != null && !request.thumbnailFilename().isBlank()
                            ? request.thumbnailFilename()
                            : "thumbnail.jpg";

            String videoKey = storageProvider.allocateKey("lessons/" + lesson.getId(), videoName);
            String thumbKey =
                    storageProvider.allocateKey("lessons/" + lesson.getId() + "/thumbnails", thumbName);

            asset.setStorageKey(videoKey);
            asset.setThumbnailStorageKey(thumbKey);
            asset.setOriginalFilename(videoName);
            asset.setMimeType(videoCt);
            asset.setThumbnailMimeType(thumbCt);
            asset.setSizeBytes(videoSize);
            asset.setUploadStatus(UploadStatus.UPLOADING);
            asset = videoAssetRepository.save(asset);

            Duration ttl = Duration.ofSeconds(
                    storageProvider instanceof S3CompatibleVideoStorageProvider s3
                            ? s3.uploadUrlTtlSeconds()
                            : s3StorageProperties.uploadUrlTtlSeconds());

            var videoPut = storageProvider.createPresignedPut(videoKey, videoCt, ttl);
            var thumbPut = storageProvider.createPresignedPut(thumbKey, thumbCt, ttl);

            auditService.record(principal.getId(), "VIDEO_UPLOAD_INIT", "VideoAsset", asset.getId(), null);
            return new UploadInitResponse(
                    asset.getId().toString(),
                    "DIRECT",
                    null,
                    videoPut.uploadUrl(),
                    thumbPut.uploadUrl(),
                    videoCt,
                    thumbCt,
                    asset.getUploadStatus().name());
        }

        asset.setStorageKey("pending/" + UUID.randomUUID());
        asset = videoAssetRepository.save(asset);
        String uploadUrl = "/api/v1/videos/" + asset.getId() + "/upload";
        auditService.record(principal.getId(), "VIDEO_UPLOAD_INIT", "VideoAsset", asset.getId(), null);
        return new UploadInitResponse(
                asset.getId().toString(),
                "PROXY",
                uploadUrl,
                null,
                null,
                null,
                null,
                asset.getUploadStatus().name());
    }

    @Transactional
    public VideoAssetResponse uploadBinary(
            UUID videoId, MultipartFile file, MultipartFile thumbnail, CustomUserDetails principal) {
        // PROXY multipart ativo mesmo com R2 (browser pode não alcançar Cloudflare/R2).
        VideoAsset asset = findOrThrow(videoId);
        if (asset.getLessonId() == null) {
            throw new BadRequestException("Vídeo sem aula associada.");
        }
        Lesson lesson = lessonService.findActiveOrThrow(asset.getLessonId());
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);

        if (asset.getUploadStatus() == UploadStatus.UPLOADED) {
            throw new BadRequestException("Este vídeo já foi enviado. Substitua criando um novo upload.");
        }
        if (file == null || file.isEmpty()) {
            markFailed(asset, "Arquivo vazio.");
            throw new BadRequestException("Arquivo de vídeo obrigatório.");
        }
        if (thumbnail == null || thumbnail.isEmpty()) {
            markFailed(asset, "Thumbnail ausente.");
            throw new BadRequestException("Thumbnail obrigatória. Envie uma imagem (JPG, PNG ou WebP).");
        }
        if (file.getSize() > storageProperties.maxFileBytes()) {
            markFailed(asset, "Arquivo excede o tamanho máximo permitido.");
            throw new BadRequestException("Arquivo excede o tamanho máximo permitido.");
        }
        if (thumbnail.getSize() > MAX_THUMBNAIL_BYTES) {
            markFailed(asset, "Thumbnail excede o tamanho máximo permitido.");
            throw new BadRequestException("Thumbnail excede 5 MB. Envie uma imagem menor.");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!ALLOWED_MIME.contains(contentType) && !contentType.startsWith("video/")) {
            markFailed(asset, "Tipo de arquivo não suportado.");
            throw new BadRequestException("Tipo de arquivo não suportado. Envie um vídeo (ex.: MP4).");
        }

        String thumbType;
        try {
            thumbType = resolveThumbnailContentType(thumbnail);
        } catch (BadRequestException e) {
            markFailed(asset, "Tipo de thumbnail não suportado.");
            throw e;
        }

        asset.setUploadStatus(UploadStatus.UPLOADING);
        if (storageProvider.supportsDirectUpload()) {
            asset.setStorageProvider(StorageProviderType.S3_COMPATIBLE);
        }
        videoAssetRepository.save(asset);

        try (InputStream in = file.getInputStream();
                InputStream thumbIn = thumbnail.getInputStream()) {
            var stored = storageProvider.store(
                    "lessons/" + lesson.getId(),
                    file.getOriginalFilename(),
                    contentType,
                    in,
                    file.getSize());
            var thumbStored = storageProvider.store(
                    "lessons/" + lesson.getId() + "/thumbnails",
                    thumbnail.getOriginalFilename() != null
                            ? thumbnail.getOriginalFilename()
                            : "thumbnail.jpg",
                    thumbType,
                    thumbIn,
                    thumbnail.getSize());

            asset.setStorageKey(stored.storageKey());
            asset.setSizeBytes(stored.sizeBytes());
            asset.setChecksum(stored.checksum());
            asset.setOriginalFilename(file.getOriginalFilename());
            asset.setMimeType(contentType);
            asset.setThumbnailStorageKey(thumbStored.storageKey());
            asset.setThumbnailMimeType(thumbType);
            attachUploadedAsset(asset, lesson);
            auditService.record(principal.getId(), "VIDEO_UPLOADED", "VideoAsset", asset.getId(), null);
            log.info(
                    "Vídeo {} (PROXY multipart) associado à aula {} ({} bytes)",
                    asset.getId(),
                    lesson.getId(),
                    asset.getSizeBytes());
            return VideoAssetResponse.from(asset);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Falha no upload do vídeo {}: {}", videoId, e.getClass().getSimpleName());
            markFailed(asset, "Falha ao armazenar o vídeo.");
            throw new BadRequestException("Não foi possível concluir o upload do vídeo.");
        }
    }

    /**
     * Define ou troca a thumbnail de um vídeo já enviado (sem reenviar o arquivo de vídeo).
     * Necessário para aulas antigas que foram publicadas antes da thumbnail obrigatória.
     */
    @Transactional
    public VideoAssetResponse uploadThumbnail(
            UUID videoId, MultipartFile thumbnail, CustomUserDetails principal) {
        VideoAsset asset = findOrThrow(videoId);
        Lesson lesson = requireManageForAsset(asset, principal);

        if (asset.getUploadStatus() != UploadStatus.UPLOADED) {
            throw new BadRequestException("Envie o vídeo antes de definir a thumbnail.");
        }
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new BadRequestException("Arquivo de thumbnail obrigatório.");
        }
        if (thumbnail.getSize() > MAX_THUMBNAIL_BYTES) {
            throw new BadRequestException("Thumbnail excede 5 MB. Envie uma imagem menor.");
        }

        String thumbType = resolveThumbnailContentType(thumbnail);
        try (InputStream thumbIn = thumbnail.getInputStream()) {
            String oldKey = asset.getThumbnailStorageKey();
            var thumbStored = storageProvider.store(
                    "lessons/" + lesson.getId() + "/thumbnails",
                    thumbnail.getOriginalFilename() != null
                            ? thumbnail.getOriginalFilename()
                            : "thumbnail.jpg",
                    thumbType,
                    thumbIn,
                    thumbnail.getSize());
            asset.setThumbnailStorageKey(thumbStored.storageKey());
            asset.setThumbnailMimeType(thumbType);
            asset = videoAssetRepository.save(asset);
            if (oldKey != null && !oldKey.isBlank()) {
                try {
                    storageProvider.delete(oldKey);
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
            auditService.record(principal.getId(), "VIDEO_THUMBNAIL_UPLOADED", "VideoAsset", asset.getId(), null);
            return VideoAssetResponse.from(asset);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(
                    "Não foi possível salvar a thumbnail: "
                            + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @Transactional
    public VideoAssetResponse completeUpload(UUID videoId, CustomUserDetails principal) {
        VideoAsset asset = findOrThrow(videoId);
        Lesson lesson = requireManageForAsset(asset, principal);

        if (asset.getUploadStatus() == UploadStatus.UPLOADED
                && asset.getProcessingStatus() == ProcessingStatus.READY) {
            return VideoAssetResponse.from(asset);
        }

        if (storageProvider.supportsDirectUpload()) {
            if (asset.getUploadStatus() != UploadStatus.UPLOADING
                    && asset.getUploadStatus() != UploadStatus.PENDING) {
                throw new BadRequestException("Upload ainda não concluído.");
            }
            if (!storageProvider.exists(asset.getStorageKey())) {
                markFailed(asset, "Objeto de vídeo ausente no storage.");
                throw new BadRequestException(
                        "Vídeo não encontrado no storage. Conclua o PUT antes do upload-complete.");
            }
            if (asset.getThumbnailStorageKey() == null
                    || asset.getThumbnailStorageKey().isBlank()
                    || !storageProvider.exists(asset.getThumbnailStorageKey())) {
                markFailed(asset, "Thumbnail ausente no storage.");
                throw new BadRequestException(
                        "Thumbnail não encontrada no storage. Conclua o PUT antes do upload-complete.");
            }
            try {
                var head = storageProvider.head(asset.getStorageKey());
                if (head.sizeBytes() > 0) {
                    asset.setSizeBytes(head.sizeBytes());
                }
                if (head.eTag() != null) {
                    asset.setChecksum(head.eTag());
                }
            } catch (Exception e) {
                log.warn("headObject falhou para {}: {}", videoId, e.getClass().getSimpleName());
            }
            attachUploadedAsset(asset, lesson);
            auditService.record(principal.getId(), "VIDEO_UPLOADED", "VideoAsset", asset.getId(), null);
            log.info(
                    "Vídeo DIRECT {} associado à aula {} ({} bytes)",
                    asset.getId(),
                    lesson.getId(),
                    asset.getSizeBytes());
            return VideoAssetResponse.from(asset);
        }

        if (asset.getUploadStatus() != UploadStatus.UPLOADED) {
            throw new BadRequestException("Upload ainda não concluído.");
        }
        if (asset.getProcessingStatus() != ProcessingStatus.READY) {
            asset.setProcessingStatus(ProcessingStatus.READY);
            asset = videoAssetRepository.save(asset);
        }
        return VideoAssetResponse.from(asset);
    }

    @Transactional(readOnly = true)
    public StreamUrlResponse streamUrl(
            UUID videoId, CustomUserDetails principal, jakarta.servlet.http.HttpServletRequest request) {
        VideoAsset asset = findOrThrow(videoId);
        requireViewAccess(asset, principal);
        if (asset.getUploadStatus() != UploadStatus.UPLOADED || asset.getProcessingStatus() != ProcessingStatus.READY) {
            throw new BadRequestException("Vídeo ainda não está pronto para reprodução.");
        }

        // S3/R2: browser toca direto no object storage (presigned GET).
        if (storageProvider.supportsDirectUpload()) {
            Duration ttl = Duration.ofSeconds(
                    storageProvider instanceof S3CompatibleVideoStorageProvider s3
                            ? s3.downloadUrlTtlSeconds()
                            : s3StorageProperties.downloadUrlTtlSeconds());
            String url = storageProvider.createPresignedGet(asset.getStorageKey(), ttl);
            String thumbnailUrl = null;
            if (asset.getThumbnailStorageKey() != null && !asset.getThumbnailStorageKey().isBlank()) {
                thumbnailUrl = storageProvider.createPresignedGet(asset.getThumbnailStorageKey(), ttl);
            }
            long expiresAt = (System.currentTimeMillis() / 1000L) + ttl.getSeconds();
            return new StreamUrlResponse(url, thumbnailUrl, expiresAt, (int) ttl.getSeconds());
        }

        var signed = streamUrlSigner.sign(videoId);
        String base = resolvePublicBaseUrl(request);
        String url = String.format(
                "%s/api/v1/videos/%s/stream?expires=%d&sig=%s",
                base,
                videoId,
                signed.expiresAt(),
                signed.signature());
        String thumbnailUrl = null;
        if (asset.getThumbnailStorageKey() != null && !asset.getThumbnailStorageKey().isBlank()) {
            thumbnailUrl = String.format(
                    "%s/api/v1/videos/%s/thumbnail?expires=%d&sig=%s",
                    base,
                    videoId,
                    signed.expiresAt(),
                    signed.signature());
        }
        return new StreamUrlResponse(url, thumbnailUrl, signed.expiresAt(), signed.ttlSeconds());
    }

    private String resolvePublicBaseUrl(jakarta.servlet.http.HttpServletRequest request) {
        if (request != null) {
            String scheme = request.getScheme();
            String host = request.getServerName();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
            String base = defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
            return trimTrailingSlash(base);
        }
        return trimTrailingSlash(apiUrlProperties.getPublicBaseUrl());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> stream(UUID videoId, long expires, String sig) {
        if (!streamUrlSigner.isValid(videoId, expires, sig)) {
            throw new ForbiddenOperationException("URL de streaming inválida ou expirada.");
        }
        VideoAsset asset = findOrThrow(videoId);
        if (asset.getUploadStatus() != UploadStatus.UPLOADED) {
            throw new NotFoundException("Vídeo não disponível.");
        }
        InputStream in = storageProvider.open(asset.getStorageKey());
        MediaType mediaType = asset.getMimeType() != null
                ? MediaType.parseMediaType(asset.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(mediaType)
                .body(new InputStreamResource(in));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<InputStreamResource> thumbnail(UUID videoId, long expires, String sig) {
        if (!streamUrlSigner.isValid(videoId, expires, sig)) {
            throw new ForbiddenOperationException("URL de thumbnail inválida ou expirada.");
        }
        VideoAsset asset = findOrThrow(videoId);
        if (asset.getThumbnailStorageKey() == null || asset.getThumbnailStorageKey().isBlank()) {
            throw new NotFoundException("Thumbnail não disponível.");
        }
        InputStream in = storageProvider.open(asset.getThumbnailStorageKey());
        MediaType mediaType = asset.getThumbnailMimeType() != null
                ? MediaType.parseMediaType(asset.getThumbnailMimeType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .contentType(mediaType)
                .body(new InputStreamResource(in));
    }

    @Transactional
    public void detach(UUID videoId, CustomUserDetails principal) {
        VideoAsset asset = findOrThrow(videoId);
        Lesson lesson = requireManageForAsset(asset, principal);
        if (asset.getLessonId() != null && asset.getId().equals(lesson.getCurrentVideoAssetId())) {
            lesson.setCurrentVideoAssetId(null);
            lessonRepository.save(lesson);
        }
        // Não apaga o registro nem o arquivo — preserva histórico (DATABASE.md §5.7).
        auditService.record(principal.getId(), "VIDEO_DETACHED", "VideoAsset", asset.getId(), null);
    }

    @Transactional(readOnly = true)
    public VideoAssetResponse get(UUID videoId, CustomUserDetails principal) {
        VideoAsset asset = findOrThrow(videoId);
        requireViewAccess(asset, principal);
        return VideoAssetResponse.from(asset);
    }

    public VideoAsset findOrThrow(UUID id) {
        return videoAssetRepository.findById(id).orElseThrow(() -> new NotFoundException("Vídeo não encontrado."));
    }

    private void attachUploadedAsset(VideoAsset asset, Lesson lesson) {
        asset.setUploadStatus(UploadStatus.UPLOADED);
        asset.setProcessingStatus(ProcessingStatus.READY);
        asset.setFailureReason(null);
        asset = videoAssetRepository.save(asset);
        lesson.setCurrentVideoAssetId(asset.getId());
        if (lesson.getDurationSeconds() == null) {
            lesson.setDurationSeconds(asset.getDurationSeconds());
        }
        lessonRepository.save(lesson);
    }

    private Lesson requireManageForAsset(VideoAsset asset, CustomUserDetails principal) {
        if (asset.getLessonId() == null) {
            throw new BadRequestException("Vídeo sem aula associada.");
        }
        Lesson lesson = lessonService.findActiveOrThrow(asset.getLessonId());
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);
        return lesson;
    }

    private void requireViewAccess(VideoAsset asset, CustomUserDetails principal) {
        if (asset.getLessonId() == null) {
            throw new ForbiddenOperationException("Sem permissão para acessar este vídeo.");
        }
        Lesson lesson = lessonService.findActiveOrThrow(asset.getLessonId());
        enrollmentAccessGuard.requireLessonContentAccess(lesson, principal);
    }

    private void markFailed(VideoAsset asset, String reason) {
        asset.setUploadStatus(UploadStatus.FAILED);
        asset.setProcessingStatus(ProcessingStatus.FAILED);
        asset.setFailureReason(reason);
        videoAssetRepository.save(asset);
    }

    private String normalizeVideoContentType(String contentType, String filename) {
        String ct = contentType != null ? contentType.trim().toLowerCase() : "";
        if (ct.isBlank() || "application/octet-stream".equals(ct)) {
            String name = filename != null ? filename.toLowerCase() : "";
            if (name.endsWith(".webm")) {
                ct = "video/webm";
            } else if (name.endsWith(".mov")) {
                ct = "video/quicktime";
            } else if (name.endsWith(".avi")) {
                ct = "video/x-msvideo";
            } else {
                ct = "video/mp4";
            }
        }
        if (!ALLOWED_MIME.contains(ct) && !ct.startsWith("video/")) {
            throw new BadRequestException("Tipo de arquivo não suportado. Envie um vídeo (ex.: MP4).");
        }
        return ct;
    }

    private String normalizeThumbnailContentType(String contentType, String filename) {
        String thumbType = contentType != null ? contentType.toLowerCase().trim() : "";
        String thumbName = filename != null ? filename.toLowerCase() : "";
        boolean thumbOk = ALLOWED_THUMBNAIL_MIME.contains(thumbType)
                || thumbName.endsWith(".jpg")
                || thumbName.endsWith(".jpeg")
                || thumbName.endsWith(".png")
                || thumbName.endsWith(".webp");
        if (!thumbOk) {
            throw new BadRequestException("Thumbnail inválida. Use JPG, PNG ou WebP.");
        }
        if (thumbType.isBlank() || !ALLOWED_THUMBNAIL_MIME.contains(thumbType)) {
            if (thumbName.endsWith(".png")) {
                return "image/png";
            }
            if (thumbName.endsWith(".webp")) {
                return "image/webp";
            }
            return "image/jpeg";
        }
        return thumbType;
    }

    private static String resolveThumbnailContentType(MultipartFile thumbnail) {
        String thumbType =
                thumbnail.getContentType() != null ? thumbnail.getContentType().toLowerCase() : "";
        String thumbName =
                thumbnail.getOriginalFilename() != null
                        ? thumbnail.getOriginalFilename().toLowerCase()
                        : "";
        boolean thumbOk = ALLOWED_THUMBNAIL_MIME.contains(thumbType)
                || thumbName.endsWith(".jpg")
                || thumbName.endsWith(".jpeg")
                || thumbName.endsWith(".png")
                || thumbName.endsWith(".webp");
        if (!thumbOk) {
            throw new BadRequestException("Thumbnail inválida. Use JPG, PNG ou WebP.");
        }
        if (thumbType.isBlank() || !ALLOWED_THUMBNAIL_MIME.contains(thumbType)) {
            if (thumbName.endsWith(".png")) {
                return "image/png";
            }
            if (thumbName.endsWith(".webp")) {
                return "image/webp";
            }
            return "image/jpeg";
        }
        return thumbType;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8090";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
