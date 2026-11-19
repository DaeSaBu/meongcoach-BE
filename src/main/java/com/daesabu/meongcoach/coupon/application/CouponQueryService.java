package com.daesabu.meongcoach.coupon.application;

import com.daesabu.meongcoach.coupon.application.provided.CouponFinder;
import com.daesabu.meongcoach.coupon.application.required.CouponRepository;
import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.exception.CouponNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryService implements CouponFinder {

	private final CouponRepository couponRepository;

	@Override
	public Coupon findCoupon(String code) {
		String normalizedCode = Coupon.normalizeCode(code);

		return couponRepository.findByCode(normalizedCode)
				.orElseThrow(CouponNotFoundException::new);
	}
}
