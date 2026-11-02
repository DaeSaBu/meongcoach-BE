package com.daesabu.meongcoach.alert.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum AlertErrorCode implements ErrorCode {

	ALERT_DELIVERY_FAILED(502, "개발자 알림을 전송하지 못했습니다."),
	;

	private final int status;
	private final String message;

	AlertErrorCode(int status, String message) {
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
