package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class GraduationSubmissionConflictException extends DomainException {

	public GraduationSubmissionConflictException(Long topicId, Throwable cause) {
		super(GraduationErrorCode.GRADUATION_SUBMISSION_CONFLICT,
				"id가 " + topicId + "인 토픽의 실습 인증 제출이 동시에 처리되었습니다.", cause);
	}
}
