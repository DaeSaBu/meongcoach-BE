package com.daesabu.meongcoach.coupon.domain;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class CouponFixture {

	private CouponFixture() {
	}

	public static Coupon unlimited(String code, int aiTrialCount) {
		return create(code, aiTrialCount, null);
	}

	public static Coupon create(String code, int aiTrialCount, Instant redeemableUntil) {
		Coupon coupon = new Coupon();
		ReflectionTestUtils.setField(coupon, "code", code);
		ReflectionTestUtils.setField(coupon, "aiTrialCount", aiTrialCount);
		ReflectionTestUtils.setField(coupon, "redeemableUntil", redeemableUntil);
		return coupon;
	}

	public static Coupon withId(Coupon coupon, Long id) {
		ReflectionTestUtils.setField(coupon, "id", id);
		return coupon;
	}
}
