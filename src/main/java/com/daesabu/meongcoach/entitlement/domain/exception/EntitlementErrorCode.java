package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum EntitlementErrorCode implements ErrorCode {

	ENTITLEMENT_PROVIDER_UNAVAILABLE(502, "이용권 정보를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요."),
	ENTITLEMENT_REQUIRED(403, "이용권이 필요한 콘텐츠입니다."),
	;

	private final int status;
	private final String message;

	EntitlementErrorCode(int status, String message) {
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
