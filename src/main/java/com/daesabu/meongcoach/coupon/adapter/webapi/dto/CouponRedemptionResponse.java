package com.daesabu.meongcoach.coupon.adapter.webapi.dto;

import com.daesabu.meongcoach.coupon.domain.CouponRedemption;

public record CouponRedemptionResponse(int aiTrialCount) {

	public static CouponRedemptionResponse from(CouponRedemption redemption) {
		return new CouponRedemptionResponse(redemption.getAiTrialCount());
	}
}
