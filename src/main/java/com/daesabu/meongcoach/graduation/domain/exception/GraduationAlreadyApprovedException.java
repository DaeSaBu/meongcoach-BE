package com.daesabu.meongcoach.graduation.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class GraduationAlreadyApprovedException extends DomainException {

	public GraduationAlreadyApprovedException(Long topicId) {
		super(GraduationErrorCode.GRADUATION_ALREADY_APPROVED, "id가 " + topicId + "인 토픽은 이미 수료했습니다.");
	}
}
