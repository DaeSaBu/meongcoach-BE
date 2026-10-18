package com.daesabu.meongcoach.media.application.provided;

/**
 * 클라이언트가 스토리지에 직접 업로드할 수 있는 영상 업로드 URL을 발급한다.
 * 모듈 경계를 넘는 값이라 enum 대신 문자열을 받고, 변환·검증은 media 모듈이 수행한다.
 */
public interface VideoUploadUrlIssuer {

	default VideoUploadUrlResult issue(Long userId, String target, String contentType, long fileSizeBytes) {
		return issue(userId, target, null, contentType, fileSizeBytes);
	}

	/**
	 * 묶음 ID를 객체 키의 사용자 ID 다음 단계에 넣어 발급한다. 묶음을 쓰지 않는 대상이면 scopeId는 null이다.
	 */
	VideoUploadUrlResult issue(Long userId, String target, Long scopeId, String contentType, long fileSizeBytes);
}
