package com.daesabu.meongcoach.shared.security;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum AppVersionErrorCode implements ErrorCode {

	APP_UPDATE_REQUIRED(426, "앱을 최신 버전으로 업데이트해야 이용할 수 있습니다."),
	APP_VERSION_INVALID(400, "앱 버전 정보 형식이 올바르지 않습니다."),
	;

	private final int status;
	private final String message;

	AppVersionErrorCode(int status, String message) {
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
