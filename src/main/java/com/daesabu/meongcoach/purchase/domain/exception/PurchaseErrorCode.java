package com.daesabu.meongcoach.purchase.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum PurchaseErrorCode implements ErrorCode {

	PURCHASE_WEBHOOK_UNAUTHORIZED(401, "구매 웹훅 인증값이 올바르지 않습니다."),
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
