package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidTrainingCalendarPeriodException extends DomainException {
	public InvalidTrainingCalendarPeriodException() {
		super(TrainingErrorCode.TRAINING_INVALID_CALENDAR_PERIOD);
	}
}
