package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidAiPlanResponseException extends DomainException {

	public InvalidAiPlanResponseException() {
		super(PlanErrorCode.PLAN_AI_INVALID_RESPONSE);
	}

	public InvalidAiPlanResponseException(Throwable cause) {
		super(PlanErrorCode.PLAN_AI_INVALID_RESPONSE, PlanErrorCode.PLAN_AI_INVALID_RESPONSE.message(), cause);
	}
}
