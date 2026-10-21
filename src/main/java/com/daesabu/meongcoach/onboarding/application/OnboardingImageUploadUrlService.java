package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.domain.OnboardingImageTarget;
import com.daesabu.meongcoach.user.application.provided.UserProfileImageUploadUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingImageUploadUrlService implements OnboardingImageUploadUrlIssuer {

	private final UserProfileImageUploadUrlIssuer userProfileImageUploadUrlIssuer;
	private final DogProfileImageUploadUrlIssuer dogProfileImageUploadUrlIssuer;

	@Override
	public OnboardingImageUploadUrlResult issue(Long userId, String target, String contentType) {
		OnboardingImageTarget imageTarget = OnboardingImageTarget.from(target);
		MediaUploadUrlResult result = issueFor(imageTarget, userId, contentType);
		return new OnboardingImageUploadUrlResult(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}

	private MediaUploadUrlResult issueFor(OnboardingImageTarget target, Long userId, String contentType) {
		if (target == OnboardingImageTarget.USER_PROFILE) {
			return userProfileImageUploadUrlIssuer.issue(userId, contentType);
		}
		return dogProfileImageUploadUrlIssuer.issue(userId, contentType);
	}
}
