package com.daesabu.meongcoach.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiFreeTrialTest {

	@Test
	void initInitializesUsedCountToZero() {
		AiFreeTrial trial = AiFreeTrial.init(1L);

		assertThat(trial.getUserId()).isEqualTo(1L);
		assertThat(trial.getUsedCount()).isZero();
	}

	@Test
	void useIncreasesUsedCount() {
		AiFreeTrial trial = AiFreeTrial.init(1L);

		trial.use();

		assertThat(trial.getUsedCount()).isEqualTo(1);
	}
}
