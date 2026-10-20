package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.media.application.provided.MediaUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.domain.OnboardingImageTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OnboardingImageUploadUrlService implements OnboardingImageUploadUrlIssuer {

	private final MediaUploadUrlIssuer mediaUploadUrlIssuer;

	@Override
	public OnboardingImageUploadUrlResult issue(Long userId, String target, String contentType) {
		OnboardingImageTarget imageTarget = OnboardingImageTarget.from(target);
		MediaUploadRequest uploadRequest = MediaUploadRequest.image(imageTarget.getMediaScope(), userId, contentType);
		MediaUploadUrlResult result = mediaUploadUrlIssuer.issue(uploadRequest);
		return new OnboardingImageUploadUrlResult(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
