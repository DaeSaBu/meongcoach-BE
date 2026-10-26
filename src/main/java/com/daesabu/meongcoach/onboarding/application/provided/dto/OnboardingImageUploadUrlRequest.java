package com.daesabu.meongcoach.onboarding.application.provided.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingImageUploadUrlRequest(
		String target,
		@NotBlank String contentType) {
}
