package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class GraduationNotPendingException extends DomainException {

	public GraduationNotPendingException(Long graduationId) {
		super(GraduationErrorCode.GRADUATION_NOT_PENDING, "id가 " + graduationId + "인 토픽 수료는 검수 대기 중이 아닙니다.");
	}
}
