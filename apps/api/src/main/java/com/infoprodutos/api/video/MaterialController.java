package com.infoprodutos.api.video;

import com.infoprodutos.api.security.CustomUserDetails;
import com.infoprodutos.api.video.dto.MaterialResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lessons/{lessonId}/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MaterialResponse> list(
            @PathVariable UUID lessonId, @AuthenticationPrincipal CustomUserDetails principal) {
        return materialService.list(lessonId, principal);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public MaterialResponse add(
            @PathVariable UUID lessonId,
            @RequestParam(value = "title", required = false) String title,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return materialService.add(lessonId, title, file, principal);
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID lessonId,
            @PathVariable UUID materialId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        materialService.delete(lessonId, materialId, principal);
        return ResponseEntity.noContent().build();
    }
}
