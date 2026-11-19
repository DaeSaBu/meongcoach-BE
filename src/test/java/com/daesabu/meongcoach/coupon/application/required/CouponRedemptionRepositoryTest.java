package com.daesabu.meongcoach.coupon.application.required;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.coupon.domain.Coupon;
import com.daesabu.meongcoach.coupon.domain.CouponFixture;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class CouponRedemptionRepositoryTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private CouponRedemptionRepository couponRedemptionRepository;

	@Autowired
	private CouponRepository couponRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void 사용자가_쿠폰을_사용한_기록이_있는지_확인한다() {
		Coupon coupon = couponRepository.save(CouponFixture.unlimited("KINTEX", 10));
		Coupon otherCoupon = couponRepository.save(CouponFixture.unlimited("OTHER", 10));
		couponRedemptionRepository.saveAndFlush(coupon.redeem(USER_ID, Instant.now()));

		assertThat(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), USER_ID)).isTrue();
		assertThat(couponRedemptionRepository.existsByCouponIdAndUserId(coupon.getId(), USER_ID + 1)).isFalse();
		assertThat(couponRedemptionRepository.existsByCouponIdAndUserId(otherCoupon.getId(), USER_ID)).isFalse();
	}

	@Test
	void 회원_ID로_트랜잭션_범위의_advisory_lock을_잡는다() {
		couponRedemptionRepository.lockByUserId(USER_ID);

		assertThat(heldAdvisoryLockCount(USER_ID)).isEqualTo(1L);
		assertThat(heldAdvisoryLockCount(USER_ID + 1)).isZero();
	}

	private long heldAdvisoryLockCount(Long userId) {
		Number count = (Number) entityManager.getEntityManager()
				.createNativeQuery("""
						SELECT count(*) FROM pg_locks
						WHERE locktype = 'advisory' AND pid = pg_backend_pid() AND granted
						AND classid::bigint = 0 AND objid::bigint = :userId AND objsubid = 1
						""")
				.setParameter("userId", userId)
				.getSingleResult();
		return count.longValue();
	}
}
