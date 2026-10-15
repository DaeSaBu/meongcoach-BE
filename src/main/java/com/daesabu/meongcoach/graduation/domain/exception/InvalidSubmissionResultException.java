package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidSubmissionResultException extends DomainException {

	public InvalidSubmissionResultException(String value) {
		super(GraduationErrorCode.GRADUATION_INVALID_SUBMISSION_RESULT, "지원하지 않는 제출 결과입니다: " + value);
	}
}
