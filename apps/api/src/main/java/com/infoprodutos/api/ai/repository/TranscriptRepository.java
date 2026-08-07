package com.infoprodutos.api.ai.repository;

import com.infoprodutos.api.ai.domain.Transcript;
import com.infoprodutos.api.ai.domain.TranscriptStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    Optional<Transcript> findByVideoAssetIdAndStatus(UUID videoAssetId, TranscriptStatus status);

    Optional<Transcript> findByVideoAssetId(UUID videoAssetId);
}
