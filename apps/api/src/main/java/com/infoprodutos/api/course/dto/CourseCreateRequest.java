package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CourseCreateRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 200, message = "título deve ter no máximo 200 caracteres")
                String title,
        @Size(max = 220, message = "slug deve ter no máximo 220 caracteres") String slug,
        String description,
        @NotNull(message = "carga horária é obrigatória")
                @DecimalMin(value = "0.5", message = "carga horária deve ser pelo menos 0,5h")
                BigDecimal workloadHours) {}
