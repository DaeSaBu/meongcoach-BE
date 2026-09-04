package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 우리 앱이 아닌 다른 애플리케이션에서 발급된 소셜 토큰인 경우. 이 검증이 없으면 공격자가
 * 자신의 앱에서 받은 유효한 토큰으로 남의 계정에 로그인할 수 있다.
 * detail에는 어느 쪽 앱 식별자도 담지 않는다.
 */
public class SocialTokenAppMismatchException extends DomainException {

	public SocialTokenAppMismatchException() {
		super(UserErrorCode.USER_SOCIAL_TOKEN_APP_MISMATCH);
	}
}
