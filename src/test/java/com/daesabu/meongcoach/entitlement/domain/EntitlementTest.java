package com.daesabu.meongcoach.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EntitlementTest {

	private static final Long USER_ID = 1L;
	private static final Instant NOW = Instant.parse("2026-10-07T00:00:00Z");
	private static final Instant NEXT_MONTH = NOW.plus(Duration.ofDays(30));
	private static final Instant LAST_MONTH = NOW.minus(Duration.ofDays(30));

	@Test
	void 만료_시각이_없는_이용권은_활성이다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, null);

		assertThat(entitlement.isActive(NOW)).isTrue();
	}

	@Test
	void 만료_시각이_지나지_않은_이용권은_활성이다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, NEXT_MONTH);

		assertThat(entitlement.isActive(NOW)).isTrue();
	}

	@Test
	void 만료_시각이_지난_이용권은_비활성이다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, LAST_MONTH);

		assertThat(entitlement.isActive(NOW)).isFalse();
	}

	@Test
	void 만료_시각이_현재와_같으면_비활성이다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, NOW);

		assertThat(entitlement.isActive(NOW)).isFalse();
	}

	@Test
	void 활성_이용권을_만료시키면_만료_시각이_현재가_되고_비활성이_된다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, null);

		entitlement.expire(NOW);

		assertThat(entitlement.getExpiresAt()).isEqualTo(NOW);
		assertThat(entitlement.isActive(NOW)).isFalse();
	}

	@Test
	void 이미_만료된_이용권을_다시_만료시키면_예외가_발생한다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, LAST_MONTH);

		assertThatThrownBy(() -> entitlement.expire(NOW))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 만료된_이용권의_만료_시각을_미래로_바꾸면_다시_활성이_된다() {
		Entitlement entitlement = Entitlement.grant(USER_ID, EntitlementType.PUPPY, LAST_MONTH);

		entitlement.changeExpiresAt(NEXT_MONTH);

		assertThat(entitlement.getExpiresAt()).isEqualTo(NEXT_MONTH);
		assertThat(entitlement.isActive(NOW)).isTrue();
	}
}
