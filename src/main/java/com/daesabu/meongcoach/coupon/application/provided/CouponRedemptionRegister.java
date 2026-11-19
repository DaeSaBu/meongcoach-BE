package com.daesabu.meongcoach.coupon.application.provided;

import com.daesabu.meongcoach.coupon.application.provided.dto.CouponRedemptionRegisterRequest;
import com.daesabu.meongcoach.coupon.domain.CouponRedemption;
import jakarta.validation.Valid;

/**
 * 쿠폰 사용 기록 변경 능력.
 */
public interface CouponRedemptionRegister {

	/**
	 * 사용자가 코드의 쿠폰을 사용한 기록을 저장하고, 쿠폰에 설정된 횟수만큼 사용자의 AI 행동 분석 부여 횟수를 늘린다.
	 * 일치하는 쿠폰이 없으면 {@code CouponNotFoundException}, 같은 쿠폰을 이미 사용했으면 {@code CouponAlreadyRedeemedException},
	 * 입력 기한이 지났으면 {@code CouponExpiredException}을 던진다.
	 */
	CouponRedemption register(Long userId, @Valid CouponRedemptionRegisterRequest request);
}
