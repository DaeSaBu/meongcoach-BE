package com.daesabu.meongcoach.purchase.adapter.integration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RevenueCat REST API v2 연동 설정. 값이 없으면 첫 구매 동기화가 아니라 기동 시점에 실패하도록 모든 필드에 제약을 건다.
 *
 * @param baseUrl   REST API v2 베이스 URL
 * @param apiKey    v2 secret API 키. Bearer 토큰으로 전송한다. 구매 조회만 하므로 customer_information:purchases:read 권한만 준다
 * @param projectId 구매를 조회할 RevenueCat 프로젝트 ID (proj로 시작)
 */
@Validated
@ConfigurationProperties("meongcoach.revenuecat")
public record RevenueCatProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotBlank String projectId
) {
}
