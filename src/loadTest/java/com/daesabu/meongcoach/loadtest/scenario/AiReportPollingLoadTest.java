package com.daesabu.meongcoach.loadtest.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.loadtest.LoadTestPlans;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * GET /api/ai/reports/{reportId} — 앱이 분석 완료를 기다리며 폴링하는 조회. 준비 단계가 계정마다 리포트 1건(UPLOADING)을 확보하고
 * 각 요청은 tokens.csv의 자기 리포트를 읽는다. 완료되지 않는 리포트라 상태 대기 없이 시간으로만 끝난다.
 */
class AiReportPollingLoadTest {

	@Test
	void AI_리포트_상세_폴링() {
		TestPlanStats stats = LoadTestPlans.runAuthenticatedGet("ai-report-polling", true,
				(preparer, account) -> "/api/ai/reports/${reportId}");

		assertThat(stats.overall().errorsCount()).isZero();
	}
}
