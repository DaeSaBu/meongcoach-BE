package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidPlanOutcomeException extends DomainException {
	public InvalidPlanOutcomeException() {
		super(PlanErrorCode.PLAN_INVALID_OUTCOME);
	}
}
