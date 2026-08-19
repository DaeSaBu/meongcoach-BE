package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;

public record LoginResponse(String accessToken, String refreshToken, boolean needsOnboarding) {

	public static LoginResponse from(SocialLoginResult result) {
		return new LoginResponse(result.token().accessToken(), result.token().refreshToken(),
				result.needsOnboarding());
	}
}
