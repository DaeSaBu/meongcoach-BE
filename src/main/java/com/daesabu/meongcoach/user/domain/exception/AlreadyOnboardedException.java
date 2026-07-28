package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AlreadyOnboardedException extends DomainException {

	public AlreadyOnboardedException() {
		super(UserErrorCode.USER_ALREADY_ONBOARDED);
	}
}
