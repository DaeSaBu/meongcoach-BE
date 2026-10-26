package com.daesabu.meongcoach.plan.domain;

import com.daesabu.meongcoach.plan.domain.exception.InvalidPlanOutcomeException;

public enum PlanOutcome {
	ACHIEVED,
	PARTIAL,
	NOT_YET;

	public static PlanOutcome from(String value) {
		try {
			return valueOf(value);
		} catch (IllegalArgumentException exception) {
			throw new InvalidPlanOutcomeException();
		}
	}
}
