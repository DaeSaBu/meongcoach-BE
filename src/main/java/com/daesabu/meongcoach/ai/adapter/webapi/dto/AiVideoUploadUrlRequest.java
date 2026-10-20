package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiVideoUploadUrlRequest(
		@NotBlank String contentType,
		@NotNull Long fileSizeBytes) {
}
