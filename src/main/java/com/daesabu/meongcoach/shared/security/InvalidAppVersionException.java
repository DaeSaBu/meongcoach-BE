package com.daesabu.meongcoach.shared.security;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class InvalidAppVersionException extends DomainException {

	public InvalidAppVersionException() {
		super(AppVersionErrorCode.APP_VERSION_INVALID);
	}
}
