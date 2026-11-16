package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.training.domain.exception.InvalidCurriculumProgressResultException;

public enum CurriculumProgressStatus {
	PENDING,
	SUCCEEDED,
	FAILED;

	public static CurriculumProgressStatus fromResult(String value) {
		if (SUCCEEDED.name().equals(value)) {
			return SUCCEEDED;
		}
		if (FAILED.name().equals(value)) {
			return FAILED;
		}
		throw new InvalidCurriculumProgressResultException();
	}
}
