package com.daesabu.meongcoach.shared.security;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프로파일별로 허용할 웹 프론트엔드 origin 패턴. relaxed binding에 따라 YAML의
 * {@code meongcoach.cors.allowed-origin-patterns}가 생성자 인자 {@code allowedOriginPatterns}에 바인딩된다.
 * test 단독 프로파일에는 cors 키가 없어 null이 들어오므로 {@code @Validated}를 두지 않고,
 * 빈 목록(모든 교차 출처 거부)으로 정규화해 기동을 막지 않는다.
 */
@ConfigurationProperties("meongcoach.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

	public CorsProperties {
		allowedOriginPatterns = List.copyOf(Objects.requireNonNullElse(allowedOriginPatterns, List.of()));
	}
}
