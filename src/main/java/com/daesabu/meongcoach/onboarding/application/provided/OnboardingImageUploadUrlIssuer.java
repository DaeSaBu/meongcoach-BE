package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.onboarding.application.provided.dto.OnboardingImageUploadUrlRequest;
import jakarta.validation.Valid;

/**
 * 온보딩 화면용 프로필 이미지 업로드 URL 발급 능력.
 */
public interface OnboardingImageUploadUrlIssuer {

	/**
	 * 보호자 또는 강아지 프로필 이미지의 업로드 URL을 발급한다. 대상이 생략되면 강아지 프로필로 발급한다.
	 */
	OnboardingImageUploadUrlResult issue(Long userId, @Valid OnboardingImageUploadUrlRequest request);
}
