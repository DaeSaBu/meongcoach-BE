package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class TopicNotFoundException extends DomainException {

	public TopicNotFoundException(Long topicId) {
		super(TrainingErrorCode.TRAINING_TOPIC_NOT_FOUND, "id가 " + topicId + "인 토픽을 찾을 수 없습니다.");
	}
}
