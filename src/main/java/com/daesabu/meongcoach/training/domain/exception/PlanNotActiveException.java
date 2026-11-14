package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanNotActiveException extends DomainException {
	public PlanNotActiveException() {
		super(PlanErrorCode.PLAN_NOT_ACTIVE);
	}
}
