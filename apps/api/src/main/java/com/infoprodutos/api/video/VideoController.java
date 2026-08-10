package com.infoprodutos.api.video;

import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.video.dto.StreamUrlResponse;
import com.infoprodutos.api.video.dto.UploadInitRequest;
import com.infoprodutos.api.video.dto.UploadInitResponse;
import com.infoprodutos.api.video.dto.VideoAssetResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/upload-init")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public UploadInitResponse uploadInit(
            @Valid @RequestBody UploadInitRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return videoService.initUpload(request, principal);
    }

    @PostMapping(path = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public VideoAssetResponse upload(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return videoService.uploadBinary(id, file, thumbnail, principal);
    }

    @PostMapping("/{id}/upload-complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public VideoAssetResponse uploadComplete(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return videoService.completeUpload(id, principal);
    }

    @PostMapping(path = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public VideoAssetResponse uploadThumbnail(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return videoService.uploadThumbnail(id, file, principal);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public VideoAssetResponse get(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        return videoService.get(id, principal);
    }

    @GetMapping("/{id}/stream-url")
    @PreAuthorize("isAuthenticated()")
    public StreamUrlResponse streamUrl(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal,
            jakarta.servlet.http.HttpServletRequest request) {
        return videoService.streamUrl(id, principal, request);
    }

    /**
     * Endpoint de reprodução com URL assinada (sem Authorization).
     * Validação feita via HMAC + expiração.
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> stream(
            @PathVariable UUID id,
            @RequestParam("expires") long expires,
            @RequestParam("sig") String sig) {
        return videoService.stream(id, expires, sig);
    }

    /**
     * Poster/capa do vídeo com a mesma assinatura HMAC do stream (sem Authorization).
     */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<InputStreamResource> thumbnail(
            @PathVariable UUID id,
            @RequestParam("expires") long expires,
            @RequestParam("sig") String sig) {
        return videoService.thumbnail(id, expires, sig);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> detach(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails principal) {
        videoService.detach(id, principal);
        return ResponseEntity.noContent().build();
    }
}
