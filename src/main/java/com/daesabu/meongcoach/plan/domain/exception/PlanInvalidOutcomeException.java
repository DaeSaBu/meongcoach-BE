package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanInvalidOutcomeException extends DomainException {

	public PlanInvalidOutcomeException(String value) {
		super(PlanErrorCode.PLAN_INVALID_OUTCOME, "플랜 마무리 결과가 올바르지 않습니다: " + value);
	}
}
