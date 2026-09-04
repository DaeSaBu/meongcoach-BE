package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 이메일이 등록되어 있지 않거나 비밀번호가 틀린 경우.
 * 어느 쪽인지 구분해 응답하면 계정 존재 여부가 드러나므로 하나의 예외로 묶는다.
 */
public class InvalidCredentialsException extends DomainException {

	public InvalidCredentialsException() {
		super(UserErrorCode.USER_INVALID_CREDENTIALS);
	}
}
