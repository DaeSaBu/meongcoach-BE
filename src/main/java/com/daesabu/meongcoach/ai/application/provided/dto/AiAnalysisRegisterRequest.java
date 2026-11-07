package com.daesabu.meongcoach.ai.application.provided.dto;

import com.daesabu.meongcoach.ai.domain.AiAnalysis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiAnalysisRegisterRequest(
		@NotBlank String contentType,
		@NotNull Long fileSizeBytes,
		@NotNull Long durationSeconds,
		@Size(max = AiAnalysis.CONTEXT_MAX_LENGTH) String context) {
}
