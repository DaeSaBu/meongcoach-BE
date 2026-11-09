package com.daesabu.meongcoach.dog.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record DogProfileImageUploadUrlRequest(
		@NotBlank String contentType) {
}
