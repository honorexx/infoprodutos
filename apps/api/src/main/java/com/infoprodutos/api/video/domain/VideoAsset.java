package com.infoprodutos.api.video.domain;

import com.infoprodutos.api.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "video_asset")
@Getter
@Setter
@NoArgsConstructor
public class VideoAsset extends AuditableEntity {

    @Column(name = "lesson_id")
    private UUID lessonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    private StorageProviderType storageProvider = StorageProviderType.LOCAL_DEV;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "checksum", length = 128)
    private String checksum;

    /** Capa/poster; obrigatória em novos uploads. */
    @Column(name = "thumbnail_storage_key", length = 500)
    private String thumbnailStorageKey;

    @Column(name = "thumbnail_mime_type", length = 100)
    private String thumbnailMimeType;
}
