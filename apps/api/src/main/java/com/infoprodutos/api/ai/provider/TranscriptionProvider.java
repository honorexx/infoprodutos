package com.infoprodutos.api.ai.provider;

import com.infoprodutos.api.ai.provider.dto.ProviderDtos.TranscriptionResult;
import com.infoprodutos.api.ai.provider.dto.ProviderDtos.VideoAssetRef;

public interface TranscriptionProvider {
    TranscriptionResult transcribe(VideoAssetRef video, String language);
}
