package com.infoprodutos.api.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateManualQuestionRequest(
        @NotBlank @Size(min = 3, max = 2000) String statement,
        String explanation,
        @NotNull String difficulty,
        String topic,
        @NotNull @Size(min = 4, max = 4) @Valid List<CreateManualQuestionRequest.OptionInput> options) {}
