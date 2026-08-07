package com.infoprodutos.api.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAiJobRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        @Min(1) @Max(10) Integer questionCount,
        @Size(max = 10) String language,
        @Size(max = 2000) String extraInstructions,
        /** Apenas perfil dev — permite E2E com texto curto sem upload real. */
        @Size(max = 20000) String devTranscriptText) {}
