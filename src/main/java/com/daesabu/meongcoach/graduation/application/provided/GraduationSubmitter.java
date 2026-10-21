package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.TopicGraduation;

/**
 * 실습 인증 영상 제출 공개 API.
 */
public interface GraduationSubmitter {

	/**
	 * 업로드를 마친 영상으로 제출한다. 검수 대기 중에는 최신 제출의 영상을 교체하고, 보완 요청 뒤에는 이전 판정을 보존하며 새 제출을 만든다.
	 * 제출마다 기록 한 줄을 남긴다. 다른 트랜잭션에서 수료를 변경했으면 제출을 취소한다.
	 * 본인이 이 토픽으로 받은 실습 인증 영상 키가 아니면 InvalidGraduationVideoKeyException, 토픽의 커리큘럼을 다 마치지 않았으면
	 * TopicNotCompletedException, 이미 수료했으면 GraduationAlreadyApprovedException, 같은 토픽의 첫 제출이 동시에 들어오면
	 * GraduationSubmissionConflictException을 던진다.
	 */
	TopicGraduation submit(Long userId, Long topicId, GraduationSubmissionRequest request);
}
