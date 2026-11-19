package com.daesabu.meongcoach.coupon.application.provided;

import com.daesabu.meongcoach.coupon.domain.Coupon;

/**
 * 쿠폰 조회 능력.
 */
public interface CouponFinder {

	/**
	 * 코드로 쿠폰을 찾는다. 코드의 앞뒤 공백과 대소문자는 구분하지 않는다. 일치하는 쿠폰이 없으면 {@code CouponNotFoundException}을 던진다.
	 */
	Coupon findCoupon(String code);
}
