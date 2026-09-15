package com.daesabu.meongcoach.loadtest.adapter.webapi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 부하 테스트 API 공유 키. {@code meongcoach.loadtest.api-key}(환경 변수 LOADTEST_API_KEY)에 바인딩된다.
 */
@ConfigurationProperties("meongcoach.loadtest")
public record LoadTestProperties(String apiKey) {

	// 키가 설정되지 않은 환경은 어떤 요청도 통과시키지 않는다(fail closed).
	// 비교는 길이·불일치 위치가 응답 시간으로 새지 않도록 상수 시간 비교를 쓴다
	public boolean matches(String candidate) {
		if (apiKey == null || apiKey.isBlank() || candidate == null) {
			return false;
		}
		byte[] expected = apiKey.getBytes(StandardCharsets.UTF_8);
		byte[] actual = candidate.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}
}
