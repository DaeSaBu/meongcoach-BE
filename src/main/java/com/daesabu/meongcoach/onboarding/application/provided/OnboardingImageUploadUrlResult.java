package com.daesabu.meongcoach.onboarding.application.provided;

/**
 * 온보딩 화면용 프로필 이미지 업로드 URL 발급 결과.
 *
 * @param uploadUrl        클라이언트가 이미지를 PUT할 presigned URL. 발급 시 지정한 Content-Type과 동일하게 보내야 한다
 * @param publicUrl        업로드 완료 후 이미지가 공개되는 URL. 온보딩 완료 요청에 이 값을 담아 등록한다
 * @param expiresInSeconds uploadUrl의 유효 시간(초)
 */
public record OnboardingImageUploadUrlResult(String uploadUrl, String publicUrl, long expiresInSeconds) {
}
