package com.daesabu.meongcoach.loadtest.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.htmlReporter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;

import com.daesabu.meongcoach.loadtest.LoadTestConfig;
import com.daesabu.meongcoach.loadtest.LoadTestPlans;
import com.daesabu.meongcoach.loadtest.RunOutput;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

/**
 * insert 시나리오 — POST /api/loadtest/accounts. 요청마다 users·local_accounts에 한 행씩 쓰고 BCrypt 해싱을 수행한다.
 * 인증이 필요 없어 계정 준비 단계가 없다. 실행 후 dev DB에 계정이 누적되므로 load-test/cleanup-loadtest-accounts.sql로 정리한다.
 */
class AccountCreateLoadTest {

	// __UUID는 JMeter 내장 함수라 Groovy 없이 요청마다 유일한 이메일을 만든다
	private static final String BODY = "{\"email\":\"lt-${__UUID}@meongcoach.test\",\"password\":\"loadtest-password\"}";

	@Test
	void 부하_테스트_계정_생성() {
		LoadTestConfig config = LoadTestConfig.load();
		RunOutput out = RunOutput.create(config, "account-create");

		DslTestPlan plan = testPlan(
				LoadTestPlans.defaults(config),
				LoadTestPlans.group("account-create", config).children(
						httpSampler("account-create", "/api/loadtest/accounts")
								.header("X-Loadtest-Key", config.requireApiKey())
								.post(BODY, ContentType.APPLICATION_JSON)),
				htmlReporter(out.reportDir().toString()),
				jtlWriter(out.dir().toString(), "results.jtl"));
		TestPlanStats stats = LoadTestPlans.run(plan, out);

		assertThat(stats.overall().errorsCount()).isZero();
	}
}
