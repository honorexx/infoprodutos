package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CourseUpdateRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 200, message = "título deve ter no máximo 200 caracteres")
                String title,
        String description,
        @Size(max = 500, message = "URL da capa deve ter no máximo 500 caracteres") String coverImageUrl,
        @NotNull(message = "carga horária é obrigatória")
                @DecimalMin(value = "0.5", message = "carga horária deve ser pelo menos 0,5h")
                BigDecimal workloadHours,
        @Min(value = 0, message = "preço não pode ser negativo") Long priceCents,
        @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal minCompletionPercentage,
        @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal minPassingScore,
        boolean certificateEnabled,
        @Positive(message = "número máximo de tentativas deve ser positivo") Integer maxQuizAttempts) {}
