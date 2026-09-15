package com.daesabu.meongcoach.loadtest;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/**
 * 부하 테스트 실행 파라미터. {@code -Dloadtest.*} 시스템 프로퍼티 → {@code LOADTEST_*} 환경 변수 → 기본값 순으로 읽는다.
 * (예: {@code loadtest.baseUrl} ↔ {@code LOADTEST_BASE_URL}) lessonId가 0이면 시나리오가 API로 첫 레슨을 찾는다.
 */
public record LoadTestConfig(String baseUrl, String apiKey, int threads, Duration rampUp, Duration duration,
		int accounts, long lessonId, Path outputDir) {

	public static LoadTestConfig load() {
		int threads = Integer.parseInt(read("loadtest.threads", "10"));
		// 리프레시 토큰 rotation 시나리오는 스레드마다 계정을 독점하므로 계정 수가 스레드 수 이상이어야 한다
		int accounts = Integer.parseInt(read("loadtest.accounts", String.valueOf(threads)));
		if (accounts < threads) {
			throw new IllegalArgumentException("loadtest.accounts(" + accounts + ")는 loadtest.threads(" + threads
					+ ") 이상이어야 합니다");
		}
		return new LoadTestConfig(
				stripTrailingSlash(read("loadtest.baseUrl", "http://localhost:8080")),
				read("loadtest.apiKey", null),
				threads,
				parseDuration(read("loadtest.rampUp", "10s")),
				parseDuration(read("loadtest.duration", "60s")),
				accounts,
				Long.parseLong(read("loadtest.lessonId", "0")),
				Path.of(read("loadtest.outputDir", "build/load-test")));
	}

	public String requireApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("계정 생성에는 공유 키가 필요합니다. -Dloadtest.apiKey 또는 LOADTEST_API_KEY를 설정하세요");
		}
		return apiKey;
	}

	private static String read(String key, String defaultValue) {
		String fromProperty = System.getProperty(key);
		if (fromProperty != null && !fromProperty.isBlank()) {
			return fromProperty;
		}
		String fromEnv = System.getenv(toEnvName(key));
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv;
		}
		return defaultValue;
	}

	// loadtest.baseUrl → LOADTEST_BASE_URL
	private static String toEnvName(String key) {
		String snake = key.replaceAll("([a-z])([A-Z])", "$1_$2").replace('.', '_');
		return snake.toUpperCase(Locale.ROOT);
	}

	// "10s", "2m", "1h"와 ISO-8601("PT1M30S")을 모두 받는다
	private static Duration parseDuration(String value) {
		String upper = value.toUpperCase(Locale.ROOT);
		if (upper.startsWith("PT")) {
			return Duration.parse(upper);
		}
		return Duration.parse("PT" + upper);
	}

	private static String stripTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
