package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidGenderException extends DomainException {

	public InvalidGenderException(String value) {
		super(UserErrorCode.USER_INVALID_GENDER, "성별 값이 올바르지 않습니다: " + value);
	}
}
