package com.daesabu.meongcoach.plan.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class PlanLessonOrderException extends DomainException {
	public PlanLessonOrderException() {
		super(PlanErrorCode.PLAN_LESSON_ORDER);
	}
}
