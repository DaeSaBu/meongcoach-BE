package com.daesabu.meongcoach.community.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PostMediaUploadUrlRequest(
		@NotBlank String mediaType,
		@NotBlank String contentType,
		@NotNull @Positive Long fileSizeBytes) {
}
