package com.daesabu.meongcoach.coupon.application;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialRegister;
import com.daesabu.meongcoach.coupon.application.provided.CouponFinder;
import com.daesabu.meongcoach.coupon.application.provided.CouponRedemptionRegister;
import com.daesabu.meongcoach.coupon.application.provided.dto.CouponRedemptionRegisterRequest;
import com.daesabu.meongcoach.coupon.application.required.CouponRedemptionRepository;
import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.CouponRedemption;
import com.daesabu.meongcoach.coupon.domain.exception.CouponAlreadyRedeemedException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class CouponRedemptionModifyService implements CouponRedemptionRegister {

	private final CouponFinder couponFinder;
	private final CouponRedemptionRepository couponRedemptionRepository;
	private final AiTrialRegister aiTrialRegister;

	@Override
	@Transactional
	public CouponRedemption register(Long userId, CouponRedemptionRegisterRequest request) {
		Coupon coupon = couponFinder.findCoupon(request.code());
		couponRedemptionRepository.lockByUserId(userId);
		if (couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
			throw new CouponAlreadyRedeemedException();
		}

		CouponRedemption redemption = couponRedemptionRepository.save(coupon.redeem(userId, Instant.now()));
		aiTrialRegister.grant(userId, redemption.getAiTrialCount());
		return redemption;
	}
}
