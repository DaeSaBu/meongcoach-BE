package com.daesabu.meongcoach.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AiTrialTest {

	private static final Long USER_ID = 7L;

	@Test
	void 처음_시작하면_30회를_부여받고_사용_횟수는_0이다() {
		AiTrial trial = AiTrial.start(USER_ID);

		assertThat(trial.getUserId()).isEqualTo(USER_ID);
		assertThat(trial.maxCount()).isEqualTo(30);
		assertThat(trial.getUsedCount()).isZero();
		assertThat(trial.remainingCount()).isEqualTo(30);
	}

	@Test
	void 사용자_없이는_시작할_수_없다() {
		assertThatThrownBy(() -> AiTrial.start(null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void 사용하면_사용_횟수가_1_늘고_잔여_횟수가_1_줄어든다() {
		AiTrial trial = AiTrialFixture.used(USER_ID, 2);

		trial.use();

		assertThat(trial.getUsedCount()).isEqualTo(3);
		assertThat(trial.remainingCount()).isEqualTo(27);
	}

	@Test
	void 부여_횟수를_모두_쓴_뒤에도_사용을_기록한다() {
		AiTrial trial = AiTrialFixture.used(USER_ID, 30);

		trial.use();

		assertThat(trial.getUsedCount()).isEqualTo(31);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 29})
	void 사용_횟수가_부여_횟수_미만이면_체험이_남아_있다(int usedCount) {
		assertThat(AiTrialFixture.used(USER_ID, usedCount).isAvailable()).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = {30, 31})
	void 사용_횟수가_부여_횟수_이상이면_체험을_소진했다(int usedCount) {
		assertThat(AiTrialFixture.used(USER_ID, usedCount).isAvailable()).isFalse();
	}

	@ParameterizedTest
	@CsvSource({"30, 0, 30", "30, 29, 1", "30, 30, 0", "35, 30, 5", "5, 3, 2"})
	void 잔여_횟수는_부여_횟수에서_사용_횟수를_뺀_값이다(int grantedCount, int usedCount, int expected) {
		assertThat(AiTrialFixture.create(USER_ID, grantedCount, usedCount).remainingCount()).isEqualTo(expected);
	}

	@Test
	void 부여_횟수를_넘겨_사용한_경우에도_잔여_횟수는_0으로_내려간다() {
		assertThat(AiTrialFixture.used(USER_ID, 31).remainingCount()).isZero();
	}

	@Test
	void 부여_횟수가_사용자마다_달라도_그_사용자의_부여_횟수로_소진을_판단한다() {
		assertThat(AiTrialFixture.create(USER_ID, 31, 30).isAvailable()).isTrue();
		assertThat(AiTrialFixture.create(USER_ID, 5, 5).isAvailable()).isFalse();
	}
}
