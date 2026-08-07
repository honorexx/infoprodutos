package com.infoprodutos.api.video.repository;

import com.infoprodutos.api.video.domain.VideoAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {
    List<VideoAsset> findByLessonIdOrderByCreatedAtDesc(UUID lessonId);
}
