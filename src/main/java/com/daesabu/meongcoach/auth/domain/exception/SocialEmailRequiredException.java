package com.daesabu.meongcoach.auth.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class SocialEmailRequiredException extends DomainException {

	public SocialEmailRequiredException() {
		super(AuthErrorCode.AUTH_SOCIAL_EMAIL_REQUIRED);
	}
}
