package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanInvalidModeException extends DomainException {

	public PlanInvalidModeException(String value) {
		super(PlanErrorCode.PLAN_INVALID_MODE, "플랜 생성 방식이 올바르지 않습니다: " + value);
	}
}
