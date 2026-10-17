package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class GraduationNotFoundException extends DomainException {

	public GraduationNotFoundException(Long graduationId) {
		super(GraduationErrorCode.GRADUATION_NOT_FOUND, "id가 " + graduationId + "인 토픽 수료를 찾을 수 없습니다.");
	}
}
