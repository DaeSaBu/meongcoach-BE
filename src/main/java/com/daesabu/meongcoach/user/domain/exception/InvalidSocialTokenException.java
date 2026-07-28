package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 소셜 제공자가 토큰을 거부한 경우. detail은 응답에 그대로 노출되므로 토큰 값을 담지 않는다.
 */
public class InvalidSocialTokenException extends DomainException {

	public InvalidSocialTokenException() {
		super(UserErrorCode.USER_INVALID_SOCIAL_TOKEN);
	}
}
