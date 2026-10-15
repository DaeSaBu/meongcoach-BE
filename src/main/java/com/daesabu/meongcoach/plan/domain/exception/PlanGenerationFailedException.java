package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanGenerationFailedException extends DomainException {

	public PlanGenerationFailedException(String detail, Throwable cause) {
		super(PlanErrorCode.PLAN_GENERATION_FAILED, detail, cause);
	}
}
