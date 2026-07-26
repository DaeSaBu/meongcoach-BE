package com.daesabu.meongcoach.user.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

	USER_INVALID_EMAIL(400, "이메일 형식이 올바르지 않습니다.");

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
