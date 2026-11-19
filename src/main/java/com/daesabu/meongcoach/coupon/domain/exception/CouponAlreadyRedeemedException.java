package com.daesabu.meongcoach.coupon.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class CouponAlreadyRedeemedException extends DomainException {

	public CouponAlreadyRedeemedException() {
		super(CouponErrorCode.COUPON_ALREADY_REDEEMED);
	}
}
