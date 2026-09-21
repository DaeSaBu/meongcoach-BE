package com.daesabu.meongcoach.auth.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class WithdrawnUserException extends DomainException {

	public WithdrawnUserException() {
		super(AuthErrorCode.USER_WITHDRAWN);
	}
}
