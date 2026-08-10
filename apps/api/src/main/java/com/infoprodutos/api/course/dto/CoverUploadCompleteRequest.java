package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CoverUploadCompleteRequest(
        @NotBlank String storageKey,
        @NotBlank String contentType) {}
