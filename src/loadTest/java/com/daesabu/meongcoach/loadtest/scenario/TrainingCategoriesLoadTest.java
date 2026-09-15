package com.daesabu.meongcoach.loadtest.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.loadtest.LoadTestPlans;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * GET /api/training/categories — 카테고리와 토픽 전량을 한 응답에 내려 페이로드가 가장 큰 조회.
 */
class TrainingCategoriesLoadTest {

	@Test
	void 훈련_카테고리_목록_조회() {
		TestPlanStats stats = LoadTestPlans.runAuthenticatedGet("training-categories", false,
				(preparer, account) -> "/api/training/categories");

		assertThat(stats.overall().errorsCount()).isZero();
	}
}
