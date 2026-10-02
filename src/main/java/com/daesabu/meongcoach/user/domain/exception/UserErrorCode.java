package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

	USER_INVALID_MBTI(400, "MBTI 값이 올바르지 않습니다."),
	USER_INVALID_GENDER(400, "성별 값이 올바르지 않습니다."),
	USER_NOT_FOUND(404, "회원을 찾을 수 없습니다."),
	USER_ALREADY_ONBOARDED(409, "이미 온보딩을 완료한 회원입니다."),
	;

	private final int status;
	private final String message;

	UserErrorCode(int status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public String message() {
		return message;
	}

	@Override
	public int status() {
		return status;
	}
}
