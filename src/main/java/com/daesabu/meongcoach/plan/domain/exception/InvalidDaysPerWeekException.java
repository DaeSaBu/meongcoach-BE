package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidDaysPerWeekException extends DomainException {

	public InvalidDaysPerWeekException() {
		super(PlanErrorCode.PLAN_INVALID_DAYS_PER_WEEK);
	}
}
