package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UnsupportedSocialProviderException extends DomainException {

	public UnsupportedSocialProviderException(String provider) {
		super(UserErrorCode.USER_UNSUPPORTED_SOCIAL_PROVIDER, "지원하지 않는 소셜 로그인 제공자입니다: " + provider);
	}
}
