package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SocialEmailRequiredException extends DomainException {

	public SocialEmailRequiredException() {
		super(UserErrorCode.USER_SOCIAL_EMAIL_REQUIRED);
	}
}
