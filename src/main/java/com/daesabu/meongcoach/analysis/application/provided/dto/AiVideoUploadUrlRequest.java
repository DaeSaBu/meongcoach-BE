package com.daesabu.meongcoach.analysis.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiVideoUploadUrlRequest(
		@NotBlank String contentType,
		@NotNull Long fileSizeBytes,
		@NotNull Long durationSeconds) {
}
