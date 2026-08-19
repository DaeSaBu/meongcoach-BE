package com.daesabu.meongcoach.onboarding.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingImageUploadUrlRequest(
		@NotBlank String target,
		@NotBlank String contentType) {
}
