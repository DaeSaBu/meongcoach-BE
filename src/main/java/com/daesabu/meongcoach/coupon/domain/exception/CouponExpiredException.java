package com.daesabu.meongcoach.coupon.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CouponExpiredException extends DomainException {

	public CouponExpiredException() {
		super(CouponErrorCode.COUPON_EXPIRED);
	}
}
