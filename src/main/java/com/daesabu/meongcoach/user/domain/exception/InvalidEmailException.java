package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidEmailException extends DomainException {

	// detail은 응답에 그대로 노출되므로 입력한 주소를 싣지 않는다
	public InvalidEmailException() {
		super(UserErrorCode.USER_INVALID_EMAIL);
	}
}
