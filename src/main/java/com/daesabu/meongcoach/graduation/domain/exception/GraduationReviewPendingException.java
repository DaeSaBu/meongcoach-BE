package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class GraduationReviewPendingException extends DomainException {
	public GraduationReviewPendingException() {
		super(GraduationErrorCode.GRADUATION_REVIEW_PENDING);
	}
}
