package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidDailyMinutesException extends DomainException {

	public InvalidDailyMinutesException() {
		super(PlanErrorCode.PLAN_INVALID_DAILY_MINUTES);
	}
}
