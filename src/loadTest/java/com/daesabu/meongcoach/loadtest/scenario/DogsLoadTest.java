package com.daesabu.meongcoach.loadtest.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.loadtest.LoadTestPlans;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * GET /api/dogs — 컬럼이 가장 많은 Dog 엔티티를 @EntityGraph 컬렉션 fetch join으로 읽는 조회. 준비 단계의 온보딩이 강아지 1마리를 만든다.
 */
class DogsLoadTest {

	@Test
	void 보유_강아지_목록_조회() {
		TestPlanStats stats = LoadTestPlans.runAuthenticatedGet("dogs", false, (preparer, account) -> "/api/dogs");

		assertThat(stats.overall().errorsCount()).isZero();
	}
}
