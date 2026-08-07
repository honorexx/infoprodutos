package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CourseCreateRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 200, message = "título deve ter no máximo 200 caracteres")
                String title,
        @Size(max = 220, message = "slug deve ter no máximo 220 caracteres") String slug,
        String description,
        @DecimalMin(value = "0", message = "carga horária não pode ser negativa") BigDecimal workloadHours) {}
