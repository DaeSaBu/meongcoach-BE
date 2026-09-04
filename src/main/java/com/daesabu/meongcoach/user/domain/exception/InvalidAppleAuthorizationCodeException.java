package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * Apple이 인가 코드를 거부한 경우(만료·재사용·다른 앱 발급). 코드는 5분 만료·1회용이라 클라이언트가
 * 다시 인증해 새 코드로 재시도해야 한다. detail은 응답에 그대로 노출되므로 코드 값을 담지 않는다.
 */
public class InvalidAppleAuthorizationCodeException extends DomainException {

	public InvalidAppleAuthorizationCodeException() {
		super(UserErrorCode.USER_INVALID_APPLE_AUTHORIZATION_CODE);
	}
}
