package com.daesabu.meongcoach.shared.security;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class AppUpdateRequiredException extends DomainException {

	public AppUpdateRequiredException() {
		super(AppVersionErrorCode.APP_UPDATE_REQUIRED);
	}
}
