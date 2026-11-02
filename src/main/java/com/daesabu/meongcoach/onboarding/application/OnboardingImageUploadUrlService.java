package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingImageUploadUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class OnboardingImageUploadUrlService implements OnboardingImageUploadUrlIssuer {

	private final DogProfileImageUploadUrlIssuer dogProfileImageUploadUrlIssuer;

	@Override
	public OnboardingImageUploadUrlResult issue(Long userId, OnboardingImageUploadUrlRequest request) {
		MediaUploadUrlResult result = dogProfileImageUploadUrlIssuer.issue(userId, request.contentType());
		return new OnboardingImageUploadUrlResult(
				result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
