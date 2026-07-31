package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidMbtiException extends DomainException {

	public InvalidMbtiException(String value) {
		super(UserErrorCode.USER_INVALID_MBTI, "MBTI 값이 올바르지 않습니다: " + value);
	}
}
