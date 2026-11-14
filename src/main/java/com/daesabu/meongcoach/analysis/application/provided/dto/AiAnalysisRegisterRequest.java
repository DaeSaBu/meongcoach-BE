package com.daesabu.meongcoach.analysis.application.provided.dto;

import com.daesabu.meongcoach.analysis.domain.AiAnalysis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiAnalysisRegisterRequest(
		@NotBlank String contentType,
		@NotNull Long fileSizeBytes,
		@NotNull Long durationSeconds,
		@Size(max = AiAnalysis.CONTEXT_MAX_LENGTH) String context) {

	public AiAnalysisRegisterRequest {
		if (context != null) {
			context = context.strip();
		}
	}
}
