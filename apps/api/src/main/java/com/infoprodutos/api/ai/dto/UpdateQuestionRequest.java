package com.infoprodutos.api.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateQuestionRequest(
        @NotBlank @Size(max = 500) String statement,
        @Size(max = 2000) String explanation,
        @NotBlank String difficulty,
        @Size(max = 255) String topic,
        @NotNull @Size(min = 4, max = 4) @Valid List<OptionInput> options) {

    public record OptionInput(@NotBlank @Size(max = 500) String text, boolean correct) {}
}
