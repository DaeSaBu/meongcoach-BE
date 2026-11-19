package com.daesabu.meongcoach.analysis.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.analysis.application.required.AiTrialRepository;
import com.daesabu.meongcoach.analysis.application.required.PurchasedTrialCountReader;
import com.daesabu.meongcoach.analysis.domain.AiTrial;
import com.daesabu.meongcoach.analysis.domain.AiTrialFixture;
import com.daesabu.meongcoach.analysis.domain.exception.AiTrialProviderUnavailableException;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AiTrialSynchronizerTest {

	private static final Long USER_ID = 7L;

	private static final Long OTHER_USER_ID = 8L;

	@Autowired
	private AiTrialSynchronizer aiTrialSynchronizer;

	@Autowired
	private PurchasedTrialCountReader purchasedTrialCountReader;

	@Autowired
	private AiTrialRepository aiTrialRepository;

	@Test
	void 현황이_없는_사용자를_동기화하면_기본_부여_횟수에_구매_횟수를_더한_현황을_저장한다() {
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(20);

		aiTrialSynchronizer.synchronize(USER_ID);

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.getPurchasedCount()).isEqualTo(20);
		assertThat(trial.maxCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 20);
		assertThat(trial.getUsedCount()).isZero();
	}

	@Test
	void 저장된_현황이_있으면_구매_횟수만_바꾸고_부여_횟수와_사용_횟수는_유지한다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.create(USER_ID, 13, 5));
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(1);

		aiTrialSynchronizer.synchronize(USER_ID);

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.getGrantedCount()).isEqualTo(13);
		assertThat(trial.getPurchasedCount()).isEqualTo(1);
		assertThat(trial.getUsedCount()).isEqualTo(5);
	}

	@Test
	void 같은_구매_횟수로_다시_동기화해도_횟수가_늘지_않는다() {
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(20);

		aiTrialSynchronizer.synchronize(USER_ID);
		aiTrialSynchronizer.synchronize(USER_ID);

		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().maxCount())
				.isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT + 20);
	}

	@Test
	void 구매가_늘면_늘어난_구매_횟수로_바꾼다() {
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(1);
		aiTrialSynchronizer.synchronize(USER_ID);
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(21);

		aiTrialSynchronizer.synchronize(USER_ID);

		assertThat(aiTrialRepository.findByUserId(USER_ID).orElseThrow().getPurchasedCount()).isEqualTo(21);
	}

	@Test
	void 환불로_구매_횟수가_줄어_사용_횟수가_최대_횟수를_넘으면_사용_횟수를_최대_횟수로_맞춘다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.purchased(USER_ID, 20, 15));
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(0);

		aiTrialSynchronizer.synchronize(USER_ID);

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.getPurchasedCount()).isZero();
		assertThat(trial.getUsedCount()).isEqualTo(AiTrial.DEFAULT_GRANTED_COUNT);
		assertThat(trial.remainingCount()).isZero();
	}

	@Test
	void 다른_사용자의_현황은_바꾸지_않는다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.purchased(OTHER_USER_ID, 5, 2));
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(20);

		aiTrialSynchronizer.synchronize(USER_ID);

		AiTrial other = aiTrialRepository.findByUserId(OTHER_USER_ID).orElseThrow();
		assertThat(other.getPurchasedCount()).isEqualTo(5);
		assertThat(other.getUsedCount()).isEqualTo(2);
	}

	@Test
	void 구매_횟수를_받아_오지_못하면_저장된_현황을_바꾸지_않는다() {
		aiTrialRepository.saveAndFlush(AiTrialFixture.purchased(USER_ID, 20, 15));
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID))
				.willThrow(new AiTrialProviderUnavailableException());

		assertThatThrownBy(() -> aiTrialSynchronizer.synchronize(USER_ID))
				.isInstanceOf(AiTrialProviderUnavailableException.class);

		AiTrial trial = aiTrialRepository.findByUserId(USER_ID).orElseThrow();
		assertThat(trial.getPurchasedCount()).isEqualTo(20);
		assertThat(trial.getUsedCount()).isEqualTo(15);
	}

	@Test
	void 요청한_사용자로_구매_횟수를_조회한다() {
		given(purchasedTrialCountReader.readPurchasedCount(USER_ID)).willReturn(0);

		aiTrialSynchronizer.synchronize(USER_ID);

		verify(purchasedTrialCountReader).readPurchasedCount(USER_ID);
	}
}
