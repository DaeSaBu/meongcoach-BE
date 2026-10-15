package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanTopicsRequiredException extends DomainException {

	public PlanTopicsRequiredException() {
		super(PlanErrorCode.PLAN_TOPICS_REQUIRED);
	}
}
