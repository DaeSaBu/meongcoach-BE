package com.daesabu.meongcoach.graduation.domain;

import com.daesabu.meongcoach.graduation.domain.exception.InvalidSubmissionResultException;

public enum SubmissionResult {
	PENDING,
	APPROVED,
	REVISION_REQUESTED,
	REPLACED;

	public static SubmissionResult from(String value) {
		if (value == null) {
			throw new InvalidSubmissionResultException(null);
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new InvalidSubmissionResultException(value);
		}
	}
}
