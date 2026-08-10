package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CoverUploadInitRequest(
        @NotBlank String contentType,
        String filename,
        @Positive Long sizeBytes) {}
