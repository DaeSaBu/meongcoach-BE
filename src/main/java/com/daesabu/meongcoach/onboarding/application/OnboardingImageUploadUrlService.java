package com.daesabu.meongcoach.onboarding.application;

import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlIssuer;
import com.daesabu.meongcoach.media.application.provided.ImageUploadUrlResult;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlIssuer;
import com.daesabu.meongcoach.onboarding.application.provided.OnboardingImageUploadUrlResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 온보딩 프로필 이미지 업로드 URL 발급을 media 모듈에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class OnboardingImageUploadUrlService implements OnboardingImageUploadUrlIssuer {

	private final ImageUploadUrlIssuer imageUploadUrlIssuer;

	@Override
	public OnboardingImageUploadUrlResult issue(Long userId, String target, String contentType) {
		ImageUploadUrlResult result = imageUploadUrlIssuer.issue(userId, target, contentType);
		return new OnboardingImageUploadUrlResult(result.uploadUrl(), result.publicUrl(), result.expiresInSeconds());
	}
}
