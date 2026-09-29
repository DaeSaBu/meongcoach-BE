package com.daesabu.meongcoach.entitlement.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum EntitlementErrorCode implements ErrorCode {

	ENTITLEMENT_UNSUPPORTED_TYPE(400, "지원하지 않는 이용권 종류입니다."),
	ENTITLEMENT_PROVIDER_UNAVAILABLE(502, "이용권 정보를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요."),
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
