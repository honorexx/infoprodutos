package com.infoprodutos.api.ai.repository;

import com.infoprodutos.api.ai.domain.TranscriptSegment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, UUID> {
    List<TranscriptSegment> findByTranscriptIdOrderBySequenceIndexAsc(UUID transcriptId);
}
