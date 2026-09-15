package com.daesabu.meongcoach.loadtest;

import static us.abstracta.jmeter.javadsl.JmeterDsl.csvDataSet;
import static us.abstracta.jmeter.javadsl.JmeterDsl.htmlReporter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpDefaults;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;
import static us.abstracta.jmeter.javadsl.JmeterDsl.uniformRandomTimer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;
import us.abstracta.jmeter.javadsl.core.configs.DslCsvDataSet;
import us.abstracta.jmeter.javadsl.core.configs.DslCsvDataSet.Sharing;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;
import us.abstracta.jmeter.javadsl.http.DslHttpDefaults;

/**
 * 시나리오가 공유하는 테스트 플랜 조각. 리포터·JMX 저장·요약 출력을 한 곳에서 처리해 시나리오 클래스는 요청 정의만 갖게 한다.
 */
public final class LoadTestPlans {

	private static final Duration THINK_TIME_MIN = Duration.ofMillis(100);
	private static final Duration THINK_TIME_MAX = Duration.ofMillis(500);

	private LoadTestPlans() {
	}

	public static DslHttpDefaults defaults(LoadTestConfig config) {
		return httpDefaults()
				.url(config.baseUrl())
				.connectionTimeout(Duration.ofSeconds(3))
				.responseTimeout(Duration.ofSeconds(10));
	}

	// 액세스 토큰은 스레드가 나눠 써도 되므로 모든 스레드가 한 포인터로 순환해서 읽는다
	public static DslCsvDataSet sharedTokens(RunOutput out) {
		return tokens(out).sharedIn(Sharing.ALL_THREADS);
	}

	// 리프레시 토큰은 1회용이라 스레드마다 한 줄씩 독점한다. 계정이 모자라면 토큰을 나눠 쓰는 대신 스레드를 멈춘다
	public static DslCsvDataSet exclusiveTokens(RunOutput out) {
		return tokens(out).sharedIn(Sharing.ALL_THREADS).stopThreadOnEOF();
	}

	private static DslCsvDataSet tokens(RunOutput out) {
		return csvDataSet(out.tokensCsv().toString())
				.ignoreFirstLine()
				.variableNames("email", "accessToken", "refreshToken", "reportId");
	}

	// rampUp 동안 threads까지 올린 뒤 duration 동안 유지한다. 반복 횟수 제한 없이 시간으로만 끝난다
	public static DslDefaultThreadGroup group(String name, LoadTestConfig config) {
		return threadGroup(name).rampToAndHold(config.threads(), config.rampUp(), config.duration());
	}

	/**
	 * 인증 GET 한 종류를 반복 호출하는 시나리오. 경로 함수는 준비된 첫 계정을 받아 필요한 ID를 API로 찾을 수 있고,
	 * 반환 경로에는 tokens.csv 변수(예: {@code ${reportId}})를 쓸 수 있다.
	 */
	public static TestPlanStats runAuthenticatedGet(String scenario, boolean withReport,
			BiFunction<AccountPreparer, PreparedAccount, String> pathResolver) {
		LoadTestConfig config = LoadTestConfig.load();
		RunOutput out = RunOutput.create(config, scenario);
		AccountPreparer preparer = new AccountPreparer(config);
		List<PreparedAccount> accounts = preparer.prepare(out, withReport);
		String path = pathResolver.apply(preparer, accounts.get(0));

		DslTestPlan plan = testPlan(
				defaults(config),
				sharedTokens(out),
				group(scenario, config).children(
						httpSampler(scenario, path).header("Authorization", "Bearer ${accessToken}"),
						uniformRandomTimer(THINK_TIME_MIN, THINK_TIME_MAX)),
				htmlReporter(out.reportDir().toString()),
				jtlWriter(out.dir().toString(), "results.jtl"));
		return run(plan, out);
	}

	/** JMX를 먼저 저장해 실행 중 예외가 나도 GUI에서 열어볼 플랜이 남게 한 뒤 실행하고 요약을 출력한다. */
	public static TestPlanStats run(DslTestPlan plan, RunOutput out) {
		try {
			plan.saveAsJmx(out.jmx().toString());
			TestPlanStats stats = plan.run();
			System.out.printf("[loadtest] samples=%d errors=%d p50=%dms p99=%dms max=%dms%n  HTML: %s%n",
					stats.overall().samplesCount(), stats.overall().errorsCount(),
					stats.overall().sampleTime().median().toMillis(), stats.overall().sampleTime().perc99().toMillis(),
					stats.overall().sampleTime().max().toMillis(), findReportIndex(out));
			return stats;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// htmlReporter는 지정한 폴더 아래에 실행 시각·UUID 이름의 하위 폴더를 만들고 그 안에 index.html을 둔다
	private static Path findReportIndex(RunOutput out) throws IOException {
		try (Stream<Path> files = Files.walk(out.reportDir(), 2)) {
			return files.filter(file -> file.getFileName().toString().equals("index.html"))
					.findFirst()
					.orElse(out.reportDir());
		}
	}
}
