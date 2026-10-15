package com.daesabu.meongcoach.plan.domain;

import com.daesabu.meongcoach.plan.domain.exception.PlanInvalidModeException;

public enum PlanMode {
	AI,
	MANUAL,
	;

	public static PlanMode from(String value) {
		if (value == null) {
			throw new PlanInvalidModeException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new PlanInvalidModeException(value);
		}
	}
}
