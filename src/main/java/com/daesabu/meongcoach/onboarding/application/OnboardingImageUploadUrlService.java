package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingImageUploadUrlRequest;
import com.daesabu.meongcoach.onboarding.domain.OnboardingImageTarget;
import com.daesabu.meongcoach.user.application.provided.UserProfileImageUploadUrlIssuer;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Validated
public class OnboardingImageUploadUrlService implements OnboardingImageUploadUrlIssuer {

	private final DogProfileImageUploadUrlIssuer dogProfileImageUploadUrlIssuer;
	private final UserProfileImageUploadUrlIssuer userProfileImageUploadUrlIssuer;

	@Override
	public OnboardingImageUploadUrlResult issue(Long userId, OnboardingImageUploadUrlRequest request) {
		OnboardingImageTarget target = OnboardingImageTarget.from(
				Objects.requireNonNullElse(request.target(), "DOG_PROFILE"));
		MediaUploadUrlResult result = issueForTarget(userId, request.contentType(), target);
		return new OnboardingImageUploadUrlResult(
				result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
	private MediaUploadUrlResult issueForTarget(Long userId, String contentType, OnboardingImageTarget target) {
		if (target == OnboardingImageTarget.USER_PROFILE) {
			return userProfileImageUploadUrlIssuer.issue(userId, contentType);
		}
		return dogProfileImageUploadUrlIssuer.issue(userId, contentType);
	}

}
