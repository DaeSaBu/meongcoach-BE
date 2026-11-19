package com.daesabu.meongcoach.coupon.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.coupon.application.provided.dto.CouponRedemptionRegisterRequest;
import com.daesabu.meongcoach.coupon.application.required.CouponRedemptionRepository;
import com.daesabu.meongcoach.coupon.application.required.CouponRepository;
import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.CouponFixture;
import com.daesabu.meongcoach.coupon.domain.CouponRedemption;
import com.daesabu.meongcoach.coupon.domain.exception.CouponAlreadyRedeemedException;
import com.daesabu.meongcoach.coupon.domain.exception.CouponExpiredException;
import com.daesabu.meongcoach.coupon.domain.exception.CouponNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class CouponRedemptionRegisterTest {

	private static final Long USER_ID = 7L;

	private static final Long OTHER_USER_ID = 8L;

	private static final String CODE = "KINTEX";

	private static final CouponRedemptionRegisterRequest REQUEST = new CouponRedemptionRegisterRequest(CODE);

	@Autowired
	private CouponRedemptionRegister couponRedemptionRegister;

	@Autowired
	private CouponRepository couponRepository;

	@Autowired
	private CouponRedemptionRepository couponRedemptionRepository;

	@Autowired
	private AiTrialFinder aiTrialFinder;

	@Test
	void 쿠폰을_사용하면_사용_기록을_저장하고_쿠폰의_횟수만큼_AI_분석_부여_횟수를_늘린다() {
		Coupon coupon = couponRepository.save(CouponFixture.unlimited(CODE, 10));

		CouponRedemption redemption = couponRedemptionRegister.register(USER_ID, REQUEST);

		assertThat(redemption.getId()).isNotNull();
		assertThat(redemption.getCouponId()).isEqualTo(coupon.getId());
		assertThat(redemption.getUserId()).isEqualTo(USER_ID);
		assertThat(redemption.getAiTrialCount()).isEqualTo(10);
		assertThat(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), USER_ID)).isTrue();
		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 10);
	}

	@ParameterizedTest
	@ValueSource(strings = {"kintex", "  KinTex  "})
	void 소문자나_앞뒤_공백이_섞인_코드로도_쿠폰을_사용한다(String code) {
		couponRepository.save(CouponFixture.unlimited(CODE, 10));

		couponRedemptionRegister.register(USER_ID, new CouponRedemptionRegisterRequest(code));

		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 10);
	}

	@Test
	void 입력_기한이_남은_쿠폰은_사용할_수_있다() {
		couponRepository.save(CouponFixture.create(CODE, 5, Instant.now().plusSeconds(3600)));

		couponRedemptionRegister.register(USER_ID, REQUEST);

		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 5);
	}

	@Test
	void 없는_코드로는_사용할_수_없다() {
		couponRepository.save(CouponFixture.unlimited(CODE, 10));

		assertThatThrownBy(() -> couponRedemptionRegister.register(USER_ID, new CouponRedemptionRegisterRequest("NOPE")))
				.isInstanceOf(CouponNotFoundException.class);
		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
	}

	@Test
	void 입력_기한이_지난_쿠폰은_사용_기록_없이_예외를_던진다() {
		Coupon coupon = couponRepository.save(CouponFixture.create(CODE, 10, Instant.now().minusSeconds(1)));

		assertThatThrownBy(() -> couponRedemptionRegister.register(USER_ID, REQUEST))
				.isInstanceOf(CouponExpiredException.class);
		assertThat(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), USER_ID)).isFalse();
		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
	}

	@Test
	void 같은_쿠폰을_다시_사용하면_예외를_던지고_부여_횟수는_한_번만_늘어난다() {
		couponRepository.save(CouponFixture.unlimited(CODE, 10));
		couponRedemptionRegister.register(USER_ID, REQUEST);

		assertThatThrownBy(() -> couponRedemptionRegister.register(USER_ID, REQUEST))
				.isInstanceOf(CouponAlreadyRedeemedException.class);
		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 10);
	}

	@Test
	void 다른_사용자는_같은_쿠폰을_사용할_수_있다() {
		couponRepository.save(CouponFixture.unlimited(CODE, 10));
		couponRedemptionRegister.register(USER_ID, REQUEST);

		couponRedemptionRegister.register(OTHER_USER_ID, REQUEST);

		assertThat(aiTrialFinder.findTrial(OTHER_USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 10);
	}

	@Test
	void 한_사용자가_서로_다른_쿠폰을_사용하면_부여_횟수가_누적된다() {
		couponRepository.save(CouponFixture.unlimited(CODE, 10));
		couponRepository.save(CouponFixture.unlimited("WELCOME", 3));

		couponRedemptionRegister.register(USER_ID, REQUEST);
		couponRedemptionRegister.register(USER_ID, new CouponRedemptionRegisterRequest("WELCOME"));

		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 13);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void 코드가_비어_있으면_검증에_실패한다(String code) {
		assertThatThrownBy(() -> couponRedemptionRegister.register(USER_ID, new CouponRedemptionRegisterRequest(code)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 코드가_50자를_넘으면_검증에_실패한다() {
		CouponRedemptionRegisterRequest request = new CouponRedemptionRegisterRequest("A".repeat(51));

		assertThatThrownBy(() -> couponRedemptionRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}
}
