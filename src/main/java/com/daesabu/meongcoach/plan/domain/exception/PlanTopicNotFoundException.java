package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanTopicNotFoundException extends DomainException {

	public PlanTopicNotFoundException(Long topicId) {
		super(PlanErrorCode.PLAN_TOPIC_NOT_FOUND, "id가 " + topicId + "인 토픽을 찾을 수 없습니다.");
	}
}
