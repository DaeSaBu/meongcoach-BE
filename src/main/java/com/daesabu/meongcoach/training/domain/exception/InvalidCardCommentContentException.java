package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidCardCommentContentException extends DomainException {

	public InvalidCardCommentContentException() {
		super(TrainingErrorCode.TRAINING_COMMENT_INVALID_CONTENT);
	}
}
