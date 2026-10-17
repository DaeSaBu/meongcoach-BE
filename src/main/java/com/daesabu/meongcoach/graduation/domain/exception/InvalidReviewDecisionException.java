package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidReviewDecisionException extends DomainException {

	public InvalidReviewDecisionException(String value) {
		super(GraduationErrorCode.GRADUATION_INVALID_REVIEW_DECISION, "지원하지 않는 판정입니다: " + value);
	}
}
