package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SubmissionNotPendingException extends DomainException {

	public SubmissionNotPendingException(Long submissionId) {
		super(GraduationErrorCode.GRADUATION_SUBMISSION_NOT_PENDING, "id가 " + submissionId + "인 제출은 검수 대기 중이 아닙니다.");
	}
}
