package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CurriculumProgressNotFoundException extends DomainException {
	public CurriculumProgressNotFoundException() {
		super(TrainingErrorCode.TRAINING_CURRICULUM_PROGRESS_NOT_FOUND);
	}
}
