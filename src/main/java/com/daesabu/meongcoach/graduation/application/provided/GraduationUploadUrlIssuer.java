package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationUploadUrlRequest;
import com.daesabu.meongcoach.media.application.provided.dto.MediaUploadUrlResult;

/**
 * 실습 인증 영상 업로드 URL 발급 공개 API.
 */
public interface GraduationUploadUrlIssuer {

	/**
	 * 회원 본인의 토픽별 실습 인증 영상 경로(videos/graduation/{userId}/{topicId}/)에 올릴 presigned URL을 발급한다. DB는 바꾸지 않는다.
	 * 토픽이 없으면 TopicNotFoundException, 토픽의 커리큘럼을 다 마치지 않았으면 TopicNotCompletedException,
	 * 검수 대기 중이면 GraduationReviewPendingException, 이미 수료한 토픽이면 GraduationAlreadyApprovedException을 던진다.
	 * 학습 완료와 제출 현황은 강아지별로 본다. 강아지 ID가 없으면 회원이 선택한 강아지로 판단한다.
	 * 지정한 강아지가 본인 소유가 아니거나 선택된 강아지가 없으면 DogNotFoundException을 던진다.
	 */
	MediaUploadUrlResult issue(Long userId, Long dogId, Long topicId, GraduationUploadUrlRequest request);
}
