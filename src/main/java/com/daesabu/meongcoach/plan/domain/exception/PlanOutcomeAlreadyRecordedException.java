package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanOutcomeAlreadyRecordedException extends DomainException {
	public PlanOutcomeAlreadyRecordedException() {
		super(PlanErrorCode.PLAN_OUTCOME_ALREADY_RECORDED);
	}
}
