package com.daesabu.meongcoach.support.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum SupportErrorCode implements ErrorCode {

	SUPPORT_CHANNEL_TALK_UNAVAILABLE(503, "문의 상담을 준비하고 있습니다.");

	private final int status;
	private final String message;

	SupportErrorCode(int status, String message) {
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
