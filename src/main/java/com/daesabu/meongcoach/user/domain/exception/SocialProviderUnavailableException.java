package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 소셜 제공자와의 통신 자체가 실패한 경우. 토큰 무효(401)와 구분해 클라이언트가
 * 재시도와 재로그인을 구별할 수 있게 한다.
 */
public class SocialProviderUnavailableException extends DomainException {

	public SocialProviderUnavailableException() {
		super(UserErrorCode.USER_SOCIAL_PROVIDER_UNAVAILABLE);
	}
}
