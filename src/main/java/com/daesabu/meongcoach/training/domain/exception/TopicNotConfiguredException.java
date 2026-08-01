package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class TopicNotConfiguredException extends DomainException {

	public TopicNotConfiguredException() {
		super(TrainingErrorCode.TRAINING_TOPIC_NOT_CONFIGURED);
	}
}
