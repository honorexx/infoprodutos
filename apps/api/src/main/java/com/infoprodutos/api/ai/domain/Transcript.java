package com.infoprodutos.api.ai.domain;

import com.infoprodutos.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcript")
@Getter
@Setter
@NoArgsConstructor
public class Transcript extends BaseEntity {

    @Column(name = "video_asset_id", nullable = false, unique = true)
    private UUID videoAssetId;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranscriptStatus status = TranscriptStatus.PENDING;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "completed_at")
    private Instant completedAt;
}
