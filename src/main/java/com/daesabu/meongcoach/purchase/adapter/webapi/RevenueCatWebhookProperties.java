package com.daesabu.meongcoach.purchase.adapter.webapi;

import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * RevenueCat 웹훅 설정. RevenueCat은 서명 없이 대시보드에 지정한 Authorization 헤더 값을 그대로 보내므로 이 값과 대조한다.
 * 필터 체인의 Bearer 토큰 필터가 JWT로 검증하려 들지 않도록 값에 {@code Bearer } 접두어를 쓰지 않는다.
 */
@Validated
@ConfigurationProperties("meongcoach.revenuecat")
public record RevenueCatWebhookProperties(@NotBlank String webhookAuthorization) {

	// 비교 시간으로 값을 추측하지 못하게 상수 시간으로 비교한다
	public boolean authorizes(String authorization) {
		if (authorization == null) {
			return false;
		}
		byte[] expected = webhookAuthorization.getBytes(StandardCharsets.UTF_8);
		byte[] actual = authorization.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}
}
