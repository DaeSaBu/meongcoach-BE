package com.daesabu.meongcoach.analysis.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.analysis.domain.AiTrialFixture;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AiTrialRegisterTest {

	private static final Long USER_ID = 7L;

	private static final Long OTHER_USER_ID = 8L;

	@Autowired
	private AiTrialRegister aiTrialRegister;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@Test
	void 현황이_없는_사용자에게_부여하면_기본_부여_횟수에_더한_현황을_저장한다() {
		aiTrialRegister.grant(USER_ID, 10);

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 10);
		assertThat(trial.getUsedCount()).isZero();
	}

	@Test
	void 저장된_현황이_있으면_부여_횟수만_늘리고_사용_횟수는_유지한다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.create(USER_ID, 30, 12));

		aiTrialRegister.grant(USER_ID, 10);

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.maxCount()).isEqualTo(40);
		assertThat(trial.getUsedCount()).isEqualTo(12);
	}

	@Test
	void 여러_번_부여하면_부여_횟수가_누적된다() {
		aiTrialRegister.grant(USER_ID, 10);
		aiTrialRegister.grant(USER_ID, 5);

		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().maxCount())
				.isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 15);
	}

	@Test
	void 다른_사용자의_현황은_바꾸지_않는다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(OTHER_USER_ID, 3));

		aiTrialRegister.grant(USER_ID, 10);

		assertThat(aiTrialRepository.findByUserId(OTHER_USER_ID).orElseThrow().maxCount())
				.isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
	}
}
