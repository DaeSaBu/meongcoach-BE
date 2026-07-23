package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidEmailException extends DomainException {

	public InvalidEmailException(String address) {
		super(UserErrorCode.USER_INVALID_EMAIL, "이메일 형식이 올바르지 않습니다: " + address);
	}
}
