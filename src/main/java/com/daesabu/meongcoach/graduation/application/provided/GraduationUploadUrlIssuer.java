package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.media.application.provided.VideoUploadUrlResult;

/**
 * 실습 인증 영상 업로드 URL 발급 공개 API.
 */
public interface GraduationUploadUrlIssuer {

	/**
	 * 회원 본인의 실습 인증 영상 경로(videos/graduation/{userId}/)에 올릴 presigned URL을 발급한다. DB는 바꾸지 않는다.
	 * 토픽이 없으면 TopicNotFoundException, 토픽의 커리큘럼을 다 마치지 않았으면 TopicNotCompletedException,
	 * 이미 수료한 토픽이면 GraduationAlreadyApprovedException을 던진다.
	 */
	VideoUploadUrlResult issue(Long userId, Long topicId, GraduationUploadUrlRequest request);
}
