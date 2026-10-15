package com.daesabu.meongcoach.plan.domain;

import com.daesabu.meongcoach.plan.domain.exception.PlanInvalidOutcomeException;

public enum PlanOutcome {
	ACHIEVED,
	PARTIAL,
	NOT_YET,
	;

	public static PlanOutcome from(String value) {
		if (value == null) {
			throw new PlanInvalidOutcomeException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new PlanInvalidOutcomeException(value);
		}
	}

	public boolean reflectsToProfile() {
		return this != NOT_YET;
	}
}
