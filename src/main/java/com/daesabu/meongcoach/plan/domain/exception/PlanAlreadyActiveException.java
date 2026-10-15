package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanAlreadyActiveException extends DomainException {

	public PlanAlreadyActiveException() {
		super(PlanErrorCode.PLAN_ALREADY_ACTIVE);
	}
}
