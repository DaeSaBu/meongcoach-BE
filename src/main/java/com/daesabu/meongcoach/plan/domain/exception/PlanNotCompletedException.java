package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanNotCompletedException extends DomainException {
	public PlanNotCompletedException() {
		super(PlanErrorCode.PLAN_NOT_COMPLETED);
	}
}
