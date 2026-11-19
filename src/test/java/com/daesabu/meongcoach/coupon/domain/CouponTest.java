package com.daesabu.meongcoach.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.coupon.domain.exception.CouponExpiredException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CouponTest {

	private static final Long COUPON_ID = 3L;

	private static final Long USER_ID = 7L;

	private static final Instant REDEEMABLE_UNTIL = Instant.parse("2026-12-01T00:00:00Z");

	@Test
	void 입력_기한이_없는_쿠폰을_사용하면_쿠폰의_횟수를_담은_사용_기록을_만든다() {
		Coupon coupon = CouponFixture.withId(CouponFixture.unlimited("KINTEX", 10), COUPON_ID);

		CouponRedemption redemption = coupon.redeem(USER_ID, Instant.parse("2030-01-01T00:00:00Z"));

		assertThat(redemption.getCouponId()).isEqualTo(COUPON_ID);
		assertThat(redemption.getUserId()).isEqualTo(USER_ID);
		assertThat(redemption.getAiTrialCount()).isEqualTo(10);
	}

	@Test
	void 입력_기한_전이면_사용할_수_있다() {
		Coupon coupon = CouponFixture.withId(CouponFixture.create("KINTEX", 10, REDEEMABLE_UNTIL), COUPON_ID);

		CouponRedemption redemption = coupon.redeem(USER_ID, REDEEMABLE_UNTIL.minusSeconds(1));

		assertThat(redemption.getAiTrialCount()).isEqualTo(10);
	}

	@ParameterizedTest
	@ValueSource(longs = {0, 1})
	void 입력_기한이_된_시각부터는_사용할_수_없다(long secondsAfter) {
		Coupon coupon = CouponFixture.withId(CouponFixture.create("KINTEX", 10, REDEEMABLE_UNTIL), COUPON_ID);

		assertThatThrownBy(() -> coupon.redeem(USER_ID, REDEEMABLE_UNTIL.plusSeconds(secondsAfter)))
				.isInstanceOf(CouponExpiredException.class);
	}

	@Test
	void 사용자_없이는_사용할_수_없다() {
		Coupon coupon = CouponFixture.withId(CouponFixture.unlimited("KINTEX", 10), COUPON_ID);

		assertThatThrownBy(() -> coupon.redeem(null, Instant.now()))
				.isInstanceOf(NullPointerException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"KINTEX", "kintex", "  KinTex \n"})
	void 코드는_앞뒤_공백을_지우고_대문자로_정규화한다(String input) {
		assertThat(Coupon.normalizeCode(input)).isEqualTo("KINTEX");
	}
}
