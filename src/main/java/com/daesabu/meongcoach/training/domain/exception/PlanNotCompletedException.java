package com.daesabu.meongcoach.training.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanNotCompletedException extends DomainException {
	public PlanNotCompletedException() {
		super(PlanErrorCode.PLAN_NOT_COMPLETED);
	}
}
