package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class DuplicatePlanCurriculumException extends DomainException {

	public DuplicatePlanCurriculumException() {
		super(PlanErrorCode.PLAN_DUPLICATE_CURRICULUM);
	}
}
