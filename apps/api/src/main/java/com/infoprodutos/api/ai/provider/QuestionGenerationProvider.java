package com.infoprodutos.api.ai.provider;

import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationInput;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationResult;

public interface QuestionGenerationProvider {
    QuestionGenerationResult generate(QuestionGenerationInput input);
}
