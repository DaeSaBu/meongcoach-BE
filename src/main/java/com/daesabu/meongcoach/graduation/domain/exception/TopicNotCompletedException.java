package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class TopicNotCompletedException extends DomainException {

	public TopicNotCompletedException(Long topicId) {
		super(GraduationErrorCode.GRADUATION_TOPIC_NOT_COMPLETED, "id가 " + topicId + "인 토픽의 커리큘럼을 아직 모두 완료하지 않았습니다.");
	}
}
