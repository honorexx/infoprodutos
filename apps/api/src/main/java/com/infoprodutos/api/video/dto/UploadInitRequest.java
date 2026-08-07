package com.infoprodutos.api.video.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UploadInitRequest(@NotNull UUID lessonId) {}
