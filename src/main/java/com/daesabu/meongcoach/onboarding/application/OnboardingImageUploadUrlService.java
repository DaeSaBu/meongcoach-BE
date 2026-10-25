package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingImageUploadUrlService implements OnboardingImageUploadUrlIssuer {

	private final DogProfileImageUploadUrlIssuer dogProfileImageUploadUrlIssuer;

	@Override
	public OnboardingImageUploadUrlResult issue(Long userId, String contentType) {
		MediaUploadUrlResult result = dogProfileImageUploadUrlIssuer.issue(userId, contentType);
		return new OnboardingImageUploadUrlResult(
				result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
