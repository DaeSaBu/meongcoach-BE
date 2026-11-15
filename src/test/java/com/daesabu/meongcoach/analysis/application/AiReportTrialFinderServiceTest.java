package com.daesabu.meongcoach.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialFinder;
import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.analysis.domain.AiTrialFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AiReportTrialFinderService.class)
class AiReportTrialFinderServiceTest {

	private static final Long USER_ID = 42L;

	private static final Long OTHER_USER_ID = 99L;

	@Autowired
	private AiTrialFinder aiTrialFinder;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@Test
	void 저장된_현황이_없으면_기본_부여_횟수에_사용_0회로_조회된다() {
		AiTrial trial = aiTrialFinder.findTrial(USER_ID);

		assertThat(trial.getUserId()).isEqualTo(USER_ID);
		assertThat(trial.maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
		assertThat(trial.getUsedCount()).isZero();
	}

	@Test
	void 조회만으로는_현황을_저장하지_않는다() {
		aiTrialFinder.findTrial(USER_ID);

		assertThat(aiTrialRepository.findByUserId(USER_ID)).isEmpty();
	}

	@Test
	void 저장된_부여_횟수와_사용_횟수를_그대로_조회한다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.create(USER_ID, 35, 12));

		AiTrial trial = aiTrialFinder.findTrial(USER_ID);

		assertThat(trial.maxCount()).isEqualTo(35);
		assertThat(trial.getUsedCount()).isEqualTo(12);
		assertThat(trial.remainingCount()).isEqualTo(23);
	}

	@Test
	void 다른_사용자의_현황은_조회하지_않는다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.used(OTHER_USER_ID, 30));

		AiTrial trial = aiTrialFinder.findTrial(USER_ID);

		assertThat(trial.getUsedCount()).isZero();
		assertThat(trial.isAvailable()).isTrue();
	}
}
