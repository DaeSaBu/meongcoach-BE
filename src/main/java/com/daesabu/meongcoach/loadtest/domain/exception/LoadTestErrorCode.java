package com.daesabu.meongcoach.loadtest.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum LoadTestErrorCode implements ErrorCode {

	LOADTEST_INVALID_KEY(403, "부하 테스트 키가 올바르지 않습니다."),
	;

	private final int status;
	private final String message;

	LoadTestErrorCode(int status, String message) {
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
