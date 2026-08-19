package com.daesabu.meongcoach.user.adapter.webapi.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 토큰 발급 요청. grantType에 따라 필수 필드가 달라서 필드별 @AssertTrue로 조건부 검증한다.
 * token은 앱이 제공자 SDK로 받은 자격증명으로, 카카오·구글·애플 모두 ID 토큰(OIDC)을 담는다.
 */
public record TokenIssueRequest(@NotNull GrantType grantType, String provider, String token, String refreshToken) {

	@AssertTrue(message = "social 발급에는 provider가 필요합니다")
	public boolean isProviderPresent() {
		return grantType != GrantType.SOCIAL || hasText(provider);
	}

	@AssertTrue(message = "social 발급에는 token이 필요합니다")
	public boolean isTokenPresent() {
		return grantType != GrantType.SOCIAL || hasText(token);
	}

	@AssertTrue(message = "refresh 발급에는 refreshToken이 필요합니다")
	public boolean isRefreshTokenPresent() {
		return grantType != GrantType.REFRESH || hasText(refreshToken);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
