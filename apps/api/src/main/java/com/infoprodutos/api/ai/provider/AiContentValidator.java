package com.infoprodutos.api.ai.provider;

import com.infoprodutos.api.ai.provider.dto.ProviderDtos.QuestionGenerationResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptSegmentDto;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.ValidationResult;
import java.util.List;

public interface AiContentValidator {
    ValidationResult validate(QuestionGenerationResult result, List<TranscriptSegmentDto> segments);
}
