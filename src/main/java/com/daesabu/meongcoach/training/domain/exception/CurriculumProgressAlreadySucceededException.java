package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CurriculumProgressAlreadySucceededException extends DomainException {
	public CurriculumProgressAlreadySucceededException() {
		super(TrainingErrorCode.TRAINING_CURRICULUM_PROGRESS_ALREADY_SUCCEEDED);
	}
}
