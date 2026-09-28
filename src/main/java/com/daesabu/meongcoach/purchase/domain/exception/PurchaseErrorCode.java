package com.daesabu.meongcoach.purchase.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum PurchaseErrorCode implements ErrorCode {

	PURCHASE_UNSUPPORTED_STORE(400, "지원하지 않는 스토어입니다."),
	;

	private final int status;
	private final String message;

	PurchaseErrorCode(int status, String message) {
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
