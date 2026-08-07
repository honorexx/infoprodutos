package com.infoprodutos.api.video;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.common.exception.NotFoundException;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.video.domain.LessonMaterial;
import com.infoprodutos.api.video.domain.StorageProviderType;
import com.infoprodutos.api.video.dto.MaterialResponse;
import com.infoprodutos.api.video.repository.LessonMaterialRepository;
import com.infoprodutos.api.video.storage.VideoStorageProvider;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final LessonMaterialRepository materialRepository;
    private final LessonService lessonService;
    private final CourseAccessGuard accessGuard;
    private final VideoStorageProvider storageProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MaterialResponse> list(UUID lessonId, CustomUserDetails principal) {
        Lesson lesson = lessonService.findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);
        return materialRepository.findByLessonIdAndDeletedAtIsNullOrderByOrderIndexAsc(lessonId).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional
    public MaterialResponse add(UUID lessonId, String title, MultipartFile file, CustomUserDetails principal) {
        Lesson lesson = lessonService.findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo do material é obrigatório.");
        }
        String materialTitle = (title == null || title.isBlank())
                ? (file.getOriginalFilename() != null ? file.getOriginalFilename() : "Material")
                : title.trim();

        try (InputStream in = file.getInputStream()) {
            var stored = storageProvider.store(
                    "materials/" + lessonId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    in,
                    file.getSize());

            LessonMaterial material = new LessonMaterial();
            material.setLessonId(lessonId);
            material.setTitle(materialTitle);
            material.setStorageProvider(StorageProviderType.LOCAL_DEV);
            material.setStorageKey(stored.storageKey());
            material.setMimeType(file.getContentType());
            material.setSizeBytes(stored.sizeBytes());
            material.setOrderIndex(
                    materialRepository.findByLessonIdAndDeletedAtIsNullOrderByOrderIndexAsc(lessonId).size());
            material = materialRepository.save(material);

            auditService.record(principal.getId(), "MATERIAL_ADDED", "LessonMaterial", material.getId(), null);
            return MaterialResponse.from(material);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Não foi possível salvar o material.");
        }
    }

    @Transactional
    public void delete(UUID lessonId, UUID materialId, CustomUserDetails principal) {
        Lesson lesson = lessonService.findActiveOrThrow(lessonId);
        accessGuard.requireManageAccess(lesson.getModule().getCourse().getId(), principal);
        LessonMaterial material = materialRepository
                .findByIdAndDeletedAtIsNull(materialId)
                .orElseThrow(() -> new NotFoundException("Material não encontrado."));
        if (!material.getLessonId().equals(lessonId)) {
            throw new NotFoundException("Material não encontrado.");
        }
        material.setDeletedAt(Instant.now());
        materialRepository.save(material);
        auditService.record(principal.getId(), "MATERIAL_DELETED", "LessonMaterial", materialId, null);
    }
}
