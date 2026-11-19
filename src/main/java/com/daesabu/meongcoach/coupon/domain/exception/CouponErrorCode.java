package com.daesabu.meongcoach.coupon.domain.exception;

import com.daesabu.meongcoach.shared.exception.ErrorCode;

public enum CouponErrorCode implements ErrorCode {

	COUPON_NOT_FOUND(404, "쿠폰 코드를 찾을 수 없습니다."),
	COUPON_EXPIRED(400, "입력 기한이 지난 쿠폰입니다."),
	COUPON_ALREADY_REDEEMED(409, "이미 사용한 쿠폰입니다.");

	private final int status;
	private final String message;

	CouponErrorCode(int status, String message) {
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
