package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class UserProfileNotFoundException extends DomainException {

	public UserProfileNotFoundException(Long userId) {
		super(UserErrorCode.USER_PROFILE_NOT_FOUND, "id가 " + userId + "인 회원의 프로필을 찾을 수 없습니다.");
	}
}
