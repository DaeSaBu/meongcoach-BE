package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class WithdrawnUserException extends DomainException {

	public WithdrawnUserException() {
		super(UserErrorCode.USER_WITHDRAWN);
	}
}
