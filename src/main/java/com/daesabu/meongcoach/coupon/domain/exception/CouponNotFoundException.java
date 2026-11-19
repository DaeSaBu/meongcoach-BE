package com.daesabu.meongcoach.coupon.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CouponNotFoundException extends DomainException {

	public CouponNotFoundException() {
		super(CouponErrorCode.COUPON_NOT_FOUND);
	}
}
