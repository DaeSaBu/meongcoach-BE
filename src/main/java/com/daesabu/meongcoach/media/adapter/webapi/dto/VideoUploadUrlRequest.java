package com.daesabu.meongcoach.media.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VideoUploadUrlRequest(
		@NotBlank String target,
		@NotBlank String contentType,
		@NotNull Long fileSizeBytes) {
}
