package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class EmptyPlanCurriculumsException extends DomainException {

	public EmptyPlanCurriculumsException() {
		super(PlanErrorCode.PLAN_EMPTY_CURRICULUMS);
	}
}
