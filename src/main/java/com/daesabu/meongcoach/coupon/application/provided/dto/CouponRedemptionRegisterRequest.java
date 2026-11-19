package com.daesabu.meongcoach.coupon.application.provided.dto;

import com.daesabu.meongcoach.coupon.domain.Coupon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CouponRedemptionRegisterRequest(
		@NotBlank @Size(max = Coupon.CODE_MAX_LENGTH) String code) {

	public CouponRedemptionRegisterRequest {
		if (code != null) {
			code = code.strip();
		}
	}
}
