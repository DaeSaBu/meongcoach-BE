package com.daesabu.meongcoach.promotion.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum PromotionErrorCode implements ErrorCode {

	PROMOTION_CODE_EXPIRED(409, "사용 기간이 지난 프로모션 코드입니다."),
	PROMOTION_CODE_EXHAUSTED(409, "사용 가능한 수량이 모두 소진된 프로모션 코드입니다."),
	;

	private final int status;
	private final String message;

	PromotionErrorCode(int status, String message) {
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
