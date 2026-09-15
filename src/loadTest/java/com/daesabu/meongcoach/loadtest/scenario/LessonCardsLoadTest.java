package com.daesabu.meongcoach.loadtest.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.loadtest.AccountPreparer;
import com.daesabu.meongcoach.loadtest.LoadTestConfig;
import com.daesabu.meongcoach.loadtest.LoadTestPlans;
import com.daesabu.meongcoach.loadtest.PreparedAccount;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * GET /api/training/lessons/{lessonId}/cards — 카드와 카드 미디어 컬렉션을 중첩 반환하는 조회.
 * 레슨 ID는 -Dloadtest.lessonId로 주고, 없으면 준비된 계정으로 첫 커리큘럼의 첫 레슨을 찾는다.
 */
class LessonCardsLoadTest {

	@Test
	void 레슨_카드_목록_조회() {
		TestPlanStats stats = LoadTestPlans.runAuthenticatedGet("lesson-cards", false,
				(preparer, account) -> "/api/training/lessons/" + resolveLessonId(preparer, account) + "/cards");

		assertThat(stats.overall().errorsCount()).isZero();
	}

	private static long resolveLessonId(AccountPreparer preparer, PreparedAccount account) {
		long configured = LoadTestConfig.load().lessonId();
		if (configured > 0) {
			return configured;
		}
		return preparer.findAnyLessonId(account.accessToken());
	}
}
