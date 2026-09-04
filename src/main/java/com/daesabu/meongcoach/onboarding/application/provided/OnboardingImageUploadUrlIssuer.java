package com.daesabu.meongcoach.onboarding.application.provided;

/**
 * 온보딩 화면용 프로필 이미지 업로드 URL 발급 능력.
 */
public interface OnboardingImageUploadUrlIssuer {

	/**
	 * 프로필 이미지(target: {@code USER_PROFILE} 또는 {@code DOG_PROFILE})의 업로드 URL을 발급한다.
	 * 지원하지 않는 target·contentType 검증은 media 모듈이 수행한다.
	 */
	OnboardingImageUploadUrlResult issue(Long userId, String target, String contentType);
}
