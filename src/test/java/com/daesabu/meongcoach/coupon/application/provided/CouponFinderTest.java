package com.daesabu.meongcoach.coupon.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.coupon.application.required.CouponRepository;
import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.CouponFixture;
import com.daesabu.meongcoach.coupon.domain.exception.CouponNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CouponFinderTest {

	@Autowired
	private CouponFinder couponFinder;

	@Autowired
	private CouponRepository couponRepository;

	@ParameterizedTest
	@ValueSource(strings = {"KINTEX", "kintex", " Kintex "})
	void 대소문자와_앞뒤_공백을_무시하고_코드로_쿠폰을_찾는다(String code) {
		Coupon saved = couponRepository.save(CouponFixture.unlimited("KINTEX", 10));

		Coupon found = couponFinder.findCoupon(code);

		assertThat(found.getId()).isEqualTo(saved.getId());
	}

	@Test
	void 없는_코드면_예외를_던진다() {
		couponRepository.save(CouponFixture.unlimited("KINTEX", 10));

		assertThatThrownBy(() -> couponFinder.findCoupon("KINTEX2"))
				.isInstanceOf(CouponNotFoundException.class);
	}
}
