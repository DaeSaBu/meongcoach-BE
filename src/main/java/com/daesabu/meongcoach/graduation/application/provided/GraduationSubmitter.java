package com.daesabu.meongcoach.graduation.application.provided;

import com.daesabu.meongcoach.graduation.application.provided.dto.GraduationSubmissionRequest;
import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;

/**
 * 실습 인증 영상 제출 공개 API.
 */
public interface GraduationSubmitter {

	/**
	 * 업로드를 마친 영상으로 제출을 만든다. 수료 행이 없으면 만들고, 상태를 제출로 바꾸고, 검수 대기 제출을 추가한 뒤 운영자에게 알린다.
	 * 이미 검수 대기 중인 제출이 있으면 그 제출을 교체로 남긴다.
	 * 본인의 실습 인증 영상 키가 아니면 InvalidGraduationVideoKeyException, 토픽의 커리큘럼을 다 마치지 않았으면
	 * TopicNotCompletedException, 이미 수료했으면 GraduationAlreadyApprovedException, 같은 토픽의 첫 제출이 동시에 들어오면
	 * GraduationSubmissionConflictException을 던진다.
	 */
	GraduationSubmission submit(Long userId, Long topicId, GraduationSubmissionRequest request);
}
