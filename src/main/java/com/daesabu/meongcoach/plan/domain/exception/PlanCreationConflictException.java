package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanCreationConflictException extends DomainException {
	public PlanCreationConflictException(Throwable cause) {
		super(PlanErrorCode.PLAN_CREATION_CONFLICT, PlanErrorCode.PLAN_CREATION_CONFLICT.message(), cause);
	}
}
