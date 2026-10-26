package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanNotFoundException extends DomainException {
	public PlanNotFoundException() {
		super(PlanErrorCode.PLAN_NOT_FOUND);
	}
}
