package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class GraduationNotApprovedException extends DomainException {

	public GraduationNotApprovedException(Long topicId) {
		super(GraduationErrorCode.GRADUATION_NOT_APPROVED, "id가 " + topicId + "인 토픽의 실습 인증이 아직 승인되지 않았습니다.");
	}
}
