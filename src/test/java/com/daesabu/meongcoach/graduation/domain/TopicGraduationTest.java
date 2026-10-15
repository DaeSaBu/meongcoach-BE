package com.daesabu.meongcoach.graduation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TopicGraduationTest {

	private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");

	@Test
	void 처음_시작한_수료는_제출_상태다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);

		assertThat(graduation.getUserId()).isEqualTo(42L);
		assertThat(graduation.getTopicId()).isEqualTo(7L);
		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
	}

	@Test
	void 보완_요청된_수료에_다시_제출하면_제출_상태로_돌아간다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);
		graduation.requestRevision();

		graduation.resubmit();

		assertThat(graduation.getStatus()).isEqualTo(GraduationStatus.SUBMITTED);
	}

	@Test
	void 승인된_수료에는_다시_제출할_수_없다() {
		TopicGraduation graduation = approved();

		assertThatThrownBy(graduation::resubmit).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 승인하면_승인_상태와_승인_시각을_기록한다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);

		graduation.approve(NOW);

		assertThat(graduation.isApproved()).isTrue();
		assertThat(graduation.getApprovedAt()).isEqualTo(NOW);
	}

	@Test
	void 제출_상태가_아니면_판정할_수_없다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);
		graduation.requestRevision();

		assertThatThrownBy(() -> graduation.approve(NOW)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(graduation::requestRevision).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 보상을_받으면_받은_시각과_만료_시각을_기록한다() {
		TopicGraduation graduation = approved();
		Instant expiresAt = NOW.plus(Duration.ofDays(3));

		graduation.claimReward(NOW, expiresAt);

		assertThat(graduation.isRewardClaimed()).isTrue();
		assertThat(graduation.getRewardClaimedAt()).isEqualTo(NOW);
		assertThat(graduation.getRewardExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void 승인되지_않은_수료는_보상을_받을_수_없다() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);

		assertThatThrownBy(() -> graduation.claimReward(NOW, NOW.plus(Duration.ofDays(3))))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 이미_받은_보상은_다시_기록할_수_없다() {
		TopicGraduation graduation = approved();
		graduation.claimReward(NOW, NOW.plus(Duration.ofDays(3)));

		assertThatThrownBy(() -> graduation.claimReward(NOW, NOW.plus(Duration.ofDays(6))))
				.isInstanceOf(IllegalStateException.class);
	}

	private TopicGraduation approved() {
		TopicGraduation graduation = TopicGraduation.start(42L, 7L);
		graduation.approve(NOW);
		return graduation;
	}
}
