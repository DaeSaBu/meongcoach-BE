package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

/**
 * 이미 로컬 계정이 등록된 이메일로 다시 계정을 만들려는 경우.
 */
public class DuplicateEmailException extends DomainException {

	public DuplicateEmailException() {
		super(UserErrorCode.USER_DUPLICATE_EMAIL);
	}
}
