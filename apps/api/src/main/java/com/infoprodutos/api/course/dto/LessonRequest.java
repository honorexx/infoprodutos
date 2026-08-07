package com.infoprodutos.api.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import com.infoprodutos.api.course.domain.LessonAccessType;

public record LessonRequest(
        @NotBlank(message = "título é obrigatório") @Size(max = 200, message = "título deve ter no máximo 200 caracteres")
                String title,
        String description,
        @PositiveOrZero(message = "duração não pode ser negativa") Integer durationSeconds,
        @NotNull(message = "tipo de acesso é obrigatório") LessonAccessType accessType) {}
