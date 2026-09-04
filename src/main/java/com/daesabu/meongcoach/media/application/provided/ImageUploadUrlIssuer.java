package com.daesabu.meongcoach.media.application.provided;

/**
 * 클라이언트가 스토리지에 직접 업로드할 수 있는 이미지 업로드 URL을 발급한다.
 * 모듈 경계를 넘는 값이라 enum 대신 문자열을 받고, 변환·검증은 media 모듈이 수행한다.
 */
public interface ImageUploadUrlIssuer {

	ImageUploadUrlResult issue(Long userId, String target, String contentType);
}
