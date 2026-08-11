package com.infoprodutos.api.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infoprodutos.api.audit.AuditService;
import com.infoprodutos.api.common.exception.BadRequestException;
import com.infoprodutos.api.config.ApiUrlProperties;
import com.infoprodutos.api.config.S3StorageProperties;
import com.infoprodutos.api.config.VideoStorageProperties;
import com.infoprodutos.api.course.CourseAccessGuard;
import com.infoprodutos.api.course.LessonService;
import com.infoprodutos.api.course.domain.Course;
import com.infoprodutos.api.course.domain.Lesson;
import com.infoprodutos.api.course.domain.Module;
import com.infoprodutos.api.course.repository.LessonRepository;
import com.infoprodutos.api.enrollment.EnrollmentAccessGuard;
import com.infoprodutos.api.enrollment.ProgressService;
import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.user.domain.Role;
import com.infoprodutos.api.user.domain.RoleCode;
import com.infoprodutos.api.user.domain.User;
import com.infoprodutos.api.video.domain.ProcessingStatus;
import com.infoprodutos.api.video.domain.UploadStatus;
import com.infoprodutos.api.video.domain.VideoAsset;
import com.infoprodutos.api.video.repository.VideoAssetRepository;
import com.infoprodutos.api.video.storage.VideoStorageProvider;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock private VideoAssetRepository videoAssetRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonService lessonService;
    @Mock private CourseAccessGuard accessGuard;
    @Mock private EnrollmentAccessGuard enrollmentAccessGuard;
    @Mock private ProgressService progressService;
    @Mock private VideoStorageProvider storageProvider;
    @Mock private StreamUrlSigner streamUrlSigner;
    @Mock private AuditService auditService;

    @Test
    void completeDirectUploadRejectsAndDeletesOversizedObject() {
        long maxBytes = 1_000L;
        VideoService service = new VideoService(
                videoAssetRepository,
                lessonRepository,
                lessonService,
                accessGuard,
                enrollmentAccessGuard,
                progressService,
                storageProvider,
                new VideoStorageProperties("./data/videos", maxBytes, 300),
                new S3StorageProperties(true, "https://r2.example", "auto", "bucket", "key", "secret", "", 3600, 300),
                streamUrlSigner,
                new ApiUrlProperties(),
                auditService);

        User instructor = new User("Professor", "prof@example.com", "hash");
        instructor.setRoles(Set.of(new Role(RoleCode.INSTRUCTOR, "Professor")));
        setId(instructor);
        Course course = new Course("Curso", "curso", instructor);
        setId(course);
        Module module = new Module(course, "Módulo", 0);
        setId(module);
        Lesson lesson = new Lesson(module, "Aula", 0);
        setId(lesson);
        VideoAsset asset = new VideoAsset();
        setId(asset);
        asset.setLessonId(lesson.getId());
        asset.setStorageKey("lessons/video.mp4");
        asset.setThumbnailStorageKey("lessons/thumb.jpg");
        asset.setMimeType("video/mp4");
        asset.setThumbnailMimeType("image/jpeg");
        asset.setUploadStatus(UploadStatus.UPLOADING);
        asset.setProcessingStatus(ProcessingStatus.PENDING);

        when(videoAssetRepository.findById(asset.getId())).thenReturn(java.util.Optional.of(asset));
        when(lessonService.findActiveOrThrow(lesson.getId())).thenReturn(lesson);
        when(storageProvider.supportsDirectUpload()).thenReturn(true);
        when(storageProvider.exists(asset.getStorageKey())).thenReturn(true);
        when(storageProvider.exists(asset.getThumbnailStorageKey())).thenReturn(true);
        when(storageProvider.head(asset.getStorageKey()))
                .thenReturn(new VideoStorageProvider.ObjectStat(maxBytes + 1, "video/mp4", "etag"));
        when(storageProvider.head(asset.getThumbnailStorageKey()))
                .thenReturn(new VideoStorageProvider.ObjectStat(100, "image/jpeg", "thumb-etag"));

        assertThatThrownBy(() -> service.completeUpload(asset.getId(), new CustomUserDetails(instructor)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tamanho máximo");

        verify(storageProvider).delete(asset.getStorageKey());
        verify(storageProvider).delete(asset.getThumbnailStorageKey());
        assertThat(asset.getUploadStatus()).isEqualTo(UploadStatus.FAILED);
    }

    private static void setId(com.infoprodutos.api.common.domain.BaseEntity entity) {
        try {
            var field = com.infoprodutos.api.common.domain.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
