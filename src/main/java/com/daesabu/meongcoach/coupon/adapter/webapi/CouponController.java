package com.daesabu.meongcoach.coupon.adapter.webapi;

import com.daesabu.meongcoach.coupon.adapter.webapi.dto.CouponRedemptionResponse;
import com.daesabu.meongcoach.coupon.application.provided.CouponRedemptionRegister;
import com.daesabu.meongcoach.coupon.application.provided.dto.CouponRedemptionRegisterRequest;
import com.daesabu.meongcoach.coupon.domain.CouponRedemption;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

	private final CouponRedemptionRegister couponRedemptionRegister;

	@PostMapping("/redemptions")
	@ResponseStatus(HttpStatus.CREATED)
	public CouponRedemptionResponse registerRedemption(@CurrentUserId Long userId,
			@Valid @RequestBody CouponRedemptionRegisterRequest request) {
		CouponRedemption redemption = couponRedemptionRegister.register(userId, request);

		return CouponRedemptionResponse.from(redemption);
	}
}
