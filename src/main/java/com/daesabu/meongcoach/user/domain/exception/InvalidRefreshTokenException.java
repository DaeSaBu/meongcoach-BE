package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 리프레시 토큰의 서명·만료·용도 검증에 실패한 경우.
 * detail은 응답에 그대로 노출되므로 토큰 값을 담지 않는다.
 */
public class InvalidRefreshTokenException extends DomainException {

	public InvalidRefreshTokenException() {
		super(UserErrorCode.USER_INVALID_REFRESH_TOKEN);
	}
}
