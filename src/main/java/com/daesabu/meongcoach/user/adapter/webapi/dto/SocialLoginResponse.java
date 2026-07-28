package com.daesabu.meongcoach.user.adapter.webapi.dto;

import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;

public record SocialLoginResponse(String accessToken, String refreshToken, boolean needsOnboarding) {

	public static SocialLoginResponse from(SocialLoginResult result) {
		return new SocialLoginResponse(result.token().accessToken(), result.token().refreshToken(),
				result.needsOnboarding());
	}
}
