package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidCurriculumProgressResultException extends DomainException {
	public InvalidCurriculumProgressResultException() {
		super(TrainingErrorCode.TRAINING_INVALID_CURRICULUM_PROGRESS_RESULT);
	}
}
