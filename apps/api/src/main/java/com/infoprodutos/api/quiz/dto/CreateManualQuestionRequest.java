package com.infoprodutos.api.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateManualQuestionRequest(
        @NotNull UUID lessonId,
        @NotBlank @Size(min = 3, max = 2000) String statement,
        String explanation,
        @NotNull String difficulty,
        String topic,
        @NotNull @Size(min = 4, max = 4) @Valid List<OptionInput> options) {

    public record OptionInput(
            @NotBlank @Size(max = 500) String text,
            @NotNull Boolean correct,
            @NotNull Integer orderIndex) {}
}
