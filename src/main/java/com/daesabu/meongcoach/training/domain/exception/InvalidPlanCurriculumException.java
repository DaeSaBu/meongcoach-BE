package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidPlanCurriculumException extends DomainException {

	public InvalidPlanCurriculumException() {
		super(PlanErrorCode.PLAN_INVALID_CURRICULUM);
	}
}
