package com.infoprodutos.api.ai.provider;

import com.infoprodutos.api.ai.provider.dto.ProviderDtos.UsageMetrics;
import java.util.UUID;

public interface AiUsageTracker {
    void recordUsage(UUID jobId, UsageMetrics metrics);
}
