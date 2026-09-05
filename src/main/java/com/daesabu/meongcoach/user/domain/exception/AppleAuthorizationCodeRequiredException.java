package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * Apple 계정으로 가입한 회원이 인가 코드 없이 탈퇴를 요청한 경우. 코드가 없으면 Apple 토큰을 revoke할 수 없어
 * 심사 요건을 어기므로 탈퇴를 진행하지 않는다.
 */
public class AppleAuthorizationCodeRequiredException extends DomainException {

	public AppleAuthorizationCodeRequiredException() {
		super(UserErrorCode.USER_APPLE_AUTHORIZATION_CODE_REQUIRED);
	}
}
