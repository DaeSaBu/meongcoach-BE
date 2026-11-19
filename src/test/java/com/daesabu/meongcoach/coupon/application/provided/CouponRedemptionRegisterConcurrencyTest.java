package com.daesabu.meongcoach.coupon.application.provided;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.coupon.application.provided.dto.CouponRedemptionRegisterRequest;
import com.daesabu.meongcoach.coupon.application.required.CouponRedemptionRepository;
import com.daesabu.meongcoach.coupon.application.required.CouponRepository;
import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.CouponFixture;
import com.daesabu.meongcoach.coupon.domain.exception.CouponAlreadyRedeemedException;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class CouponRedemptionRegisterConcurrencyTest {

	private static final Long USER_ID = 737002L;

	private static final String CODE = "DAE737LOCK";

	private static final CouponRedemptionRegisterRequest REQUEST = new CouponRedemptionRegisterRequest(CODE);

	@Autowired
	private CouponRedemptionRegister couponRedemptionRegister;

	@Autowired
	private CouponRepository couponRepository;

	@Autowired
	private CouponRedemptionRepository couponRedemptionRepository;

	@Autowired
	private AiTrialFinder aiTrialFinder;

	@Autowired
	private TransactionTemplate transactionTemplate;

	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final CompletableFuture<Void> lockReleased = new CompletableFuture<>();

	@AfterEach
	void tearDown() {
		lockReleased.complete(null);
		executor.close();
		couponRedemptionRepository.deleteAll();
		couponRepository.deleteAll();
	}

	@Test
	void 같은_사용자가_같은_쿠폰을_사용하는_중이면_끝날_때까지_기다린_뒤_이미_사용한_쿠폰으로_거절한다() throws Exception {
		Coupon coupon = couponRepository.save(CouponFixture.unlimited(CODE, 10));
		redeemHoldingLock(coupon);

		Future<?> registration = executor.submit(() -> couponRedemptionRegister.register(USER_ID, REQUEST));

		assertThatThrownBy(() -> registration.get(500, MILLISECONDS))
				.isInstanceOf(TimeoutException.class);
		lockReleased.complete(null);
		assertThatThrownBy(() -> registration.get(5, SECONDS))
				.hasCauseInstanceOf(CouponAlreadyRedeemedException.class);
		assertThat(aiTrialFinder.findTrial(USER_ID).maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
	}

	private void redeemHoldingLock(Coupon coupon) throws Exception {
		CompletableFuture<Void> lockAcquired = new CompletableFuture<>();
		executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
			couponRedemptionRepository.lockByUserId(USER_ID);
			couponRedemptionRepository.saveAndFlush(coupon.redeem(USER_ID, Instant.now()));
			lockAcquired.complete(null);
			lockReleased.join();
		}));
		lockAcquired.get(5, SECONDS);
	}
}
