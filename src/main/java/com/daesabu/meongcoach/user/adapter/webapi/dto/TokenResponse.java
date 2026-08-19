package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 토큰 발급 응답. needsOnboarding은 소셜 로그인에서만 의미가 있으므로 refresh 발급에서는 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(String accessToken, String refreshToken, Boolean needsOnboarding) {

	public static TokenResponse from(SocialLoginResult result) {
		return new TokenResponse(result.token().accessToken(), result.token().refreshToken(),
				result.needsOnboarding());
	}

	public static TokenResponse from(AuthToken token) {
		return new TokenResponse(token.accessToken(), token.refreshToken(), null);
	}
}
