package com.daesabu.meongcoach.media.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

public record ImageUploadUrlRequest(
		@NotBlank String target,
		@NotBlank String contentType) {
}
