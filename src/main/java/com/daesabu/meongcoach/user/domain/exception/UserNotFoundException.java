package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UserNotFoundException extends DomainException {

	public UserNotFoundException(Long userId) {
		super(UserErrorCode.USER_NOT_FOUND, "id가 " + userId + "인 회원을 찾을 수 없습니다.");
	}
}
