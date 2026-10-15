package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanExpectationRequiredException extends DomainException {

	public PlanExpectationRequiredException() {
		super(PlanErrorCode.PLAN_EXPECTATION_REQUIRED);
	}
}
