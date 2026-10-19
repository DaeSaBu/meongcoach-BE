package com.daesabu.meongcoach.media.application.provided;

/**
 * 사용자와 토픽에 해당하는 실습 인증 영상 업로드 URL을 발급한다.
 */
public interface GraduationVideoUploadUrlIssuer {

	VideoUploadUrlResult issue(Long userId, Long topicId, String contentType, long fileSizeBytes);
}
