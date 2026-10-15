package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SubmissionNotFoundException extends DomainException {

	public SubmissionNotFoundException(Long submissionId) {
		super(GraduationErrorCode.GRADUATION_SUBMISSION_NOT_FOUND, "id가 " + submissionId + "인 실습 인증 제출을 찾을 수 없습니다.");
	}
}
