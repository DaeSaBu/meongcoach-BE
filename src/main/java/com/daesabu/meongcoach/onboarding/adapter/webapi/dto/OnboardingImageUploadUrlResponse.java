package com.daesabu.meongcoach.onboarding.adapter.webapi.dto;

import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;

public record OnboardingImageUploadUrlResponse(String uploadUrl, String publicUrl, long expiresInSeconds) {

	public static OnboardingImageUploadUrlResponse from(OnboardingImageUploadUrlResult result) {
		return new OnboardingImageUploadUrlResponse(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
