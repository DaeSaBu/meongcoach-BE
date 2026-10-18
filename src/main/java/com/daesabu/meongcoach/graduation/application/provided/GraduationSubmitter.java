package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

/**
 * 실습 인증 영상 제출 공개 API.
 */
public interface GraduationSubmitter {

	/**
	 * 업로드를 마친 영상으로 제출한다. 수료 행이 없으면 1번째 제출로 만들고, 있으면 차수를 올리며 영상과 제출 시각을 바꾸고 이전 판정을 지운다.
	 * 제출마다 기록 한 줄을 남긴다.
	 * 본인이 이 토픽으로 받은 실습 인증 영상 키가 아니면 InvalidGraduationVideoKeyException, 토픽의 커리큘럼을 다 마치지 않았으면
	 * TopicNotCompletedException, 이미 수료했으면 GraduationAlreadyApprovedException, 같은 토픽의 첫 제출이 동시에 들어오면
	 * GraduationSubmissionConflictException을 던진다.
	 */
	TopicGraduation submit(Long userId, Long topicId, GraduationSubmissionRequest request);
}
