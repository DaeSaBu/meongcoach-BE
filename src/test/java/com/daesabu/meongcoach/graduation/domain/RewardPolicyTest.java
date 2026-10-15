package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RewardPolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");

	@Test
	void 이전_보상이_없으면_지금부터_3일이다() {
		assertThat(RewardPolicy.expiresAt(NOW, null)).isEqualTo(NOW.plus(Duration.ofDays(3)));
	}

	@Test
	void 이전_보상이_남아_있으면_그_만료_시각_뒤로_3일을_잇는다() {
		Instant previous = NOW.plus(Duration.ofDays(2));

		assertThat(RewardPolicy.expiresAt(NOW, previous)).isEqualTo(previous.plus(Duration.ofDays(3)));
	}

	@Test
	void 이전_보상이_이미_끝났으면_지금부터_3일이다() {
		Instant previous = NOW.minus(Duration.ofDays(1));

		assertThat(RewardPolicy.expiresAt(NOW, previous)).isEqualTo(NOW.plus(Duration.ofDays(3)));
	}

	@Test
	void 보상_일수는_3일이다() {
		assertThat(RewardPolicy.DAYS).isEqualTo(3);
	}
}
