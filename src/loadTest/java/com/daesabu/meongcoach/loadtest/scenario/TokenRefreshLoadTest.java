package com.daesabu.meongcoach.loadtest.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.constantTimer;
import static us.abstracta.jmeter.javadsl.JmeterDsl.htmlReporter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jsonExtractor;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.onceOnlyController;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;

import com.daesabu.meongcoach.loadtest.AccountPreparer;
import com.daesabu.meongcoach.loadtest.LoadTestConfig;
import com.daesabu.meongcoach.loadtest.LoadTestPlans;
import com.daesabu.meongcoach.loadtest.RunOutput;
import java.time.Duration;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * POST /api/auth/token/refresh — refresh_tokens를 SELECT·UPDATE(폐기)·INSERT(재발급)하는 유일한 API.
 * 리프레시 토큰은 1회용(rotation)이라 스레드마다 계정 하나를 독점하고, 응답의 새 토큰을 추출해 다음 요청에 쓴다.
 * 한 번이라도 실패하면 그 스레드는 폐기된 토큰으로 401이 연쇄되므로 리포트를 "첫 실패 이후 연쇄"로 읽는다.
 */
class TokenRefreshLoadTest {

	private static final Duration INTERVAL = Duration.ofMillis(200);

	@Test
	void 리프레시_토큰_재발급_rotation() {
		LoadTestConfig config = LoadTestConfig.load();
		RunOutput out = RunOutput.create(config, "token-refresh");
		new AccountPreparer(config).prepare(out, false);

		DslTestPlan plan = testPlan(
				LoadTestPlans.defaults(config),
				LoadTestPlans.group("token-refresh", config).children(
						// OnceOnly 아래의 CSV는 스레드당 한 번만 다음 줄을 읽고, 이후 값은 추출기가 rotation 결과로 덮어쓴다
						onceOnlyController(LoadTestPlans.exclusiveTokens(out)),
						httpSampler("token-refresh", "/api/auth/token/refresh")
								.post("{\"refreshToken\":\"${refreshToken}\"}", ContentType.APPLICATION_JSON)
								.children(
										// 기본 질의 언어는 JMESPath라 "$." 없이 필드명만 쓴다
										jsonExtractor("refreshToken", "refreshToken"),
										jsonExtractor("accessToken", "accessToken")),
						constantTimer(INTERVAL)),
				htmlReporter(out.reportDir().toString()),
				jtlWriter(out.dir().toString(), "results.jtl"));
		TestPlanStats stats = LoadTestPlans.run(plan, out);

		assertThat(stats.overall().errorsCount()).isZero();
	}
}
