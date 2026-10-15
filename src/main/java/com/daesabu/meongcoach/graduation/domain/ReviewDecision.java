package com.daesabu.meongcoach.graduation.domain;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidReviewDecisionException;

public enum ReviewDecision {
	APPROVED,
	REVISION_REQUESTED;

	public static ReviewDecision from(String value) {
		if (value == null) {
			throw new InvalidReviewDecisionException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidReviewDecisionException(value);
		}
	}
}
